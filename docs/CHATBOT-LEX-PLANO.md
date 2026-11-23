# Plano: chatbot real (Amazon Lex V2 + Bedrock opcional)

Escopo: sair do modo `mock` para Lex real, **qualificando o lead** (gravando no `Lead`) e entregando-o ao vendedor. Referências de código abaixo são relativas a `backend/src/main/kotlin/com/astro/leadflow/shared/modules/`.
Valores de custo e suporte de recursos AWS marcados com (confirmar) vêm de memória e precisam de checagem na documentação/calculadora antes de decidir.

---

## 1. Estado atual

### O que existe e funciona (modo mock)
- Pipeline completo: Meta webhook → `communication/api/MetaWebhookController` → `communication/service/BotService.handleIncoming:28` → `chatbot/routing/ChatbotRouter.handle:33` → resposta via `MessageSenderService.sendText`.
- `EngineSelector` escolhe motor pelo tier do plano (`NONE`→TEMPLATE, `LEX_ONLY`, `LEX_BEDROCK_FALLBACK`) (`routing/EngineSelector.kt:15-19`).
- Sessão (`BotSession`, jsonb `slot_values`), cache Redis 30 min (`session/BotContextCache.kt:31`), `HandoffEvaluator`, quota/tracking de tokens, `ChatbotConfig` com `botId/botAliasId/localeId/modelId` já mapeados em `application.yaml` (envs `AWS_LEX_BOT_ID`, `AWS_LEX_BOT_ALIAS_ID`, `AWS_LEX_LOCALE_ID`, `AWS_BEDROCK_MODEL_ID`, `CHATBOT_ENGINE_MODE`).
- SDKs `lexruntimev2` e `bedrockruntime` já no `build.gradle.kts:60-61`.
- Testes para router, selector, handoff, sessão, cache, quota (`src/test/.../chatbot/**`).

### O que é stub
| Item | Prova |
|---|---|
| `AwsLexEngine` não chama o Lex | `engine/aws/AwsLexEngine.kt:15-19` devolve texto fixo + `FALLBACK` |
| `AwsBedrockEngine` idem | `engine/aws/AwsBedrockEngine.kt:15-19` |
| Nenhum bean `LexRuntimeV2Client`/`BedrockRuntimeClient` | só `SqsConfig` (`shared/config/SqsConfig.kt`) existe como padrão de client AWS |
| Bot Lex não existe na AWS | nenhum IaC no repo (`infra/` só tem `monitoring/`) |

### Gap principal: os slots NÃO chegam ao Lead (verificado)
- `BotSessionService.applyOutput:33` faz `session.slotValues.putAll(output.slotValues)` → só grava em `bot_session.slot_values` e no Redis (`ChatbotRouter.kt:49`).
- Único consumidor de `slotValues` fora do chatbot: `ChatbotManagerController.kt:70` (exibição read-only).
- Os campos `Lead.vehicleInterest/budget/urgency/purchaseForecast/interestType/notes` só são escritos por `crm/service/LeadService.kt:73-78` (`update`), que depende de `SecurityUtils.getCurrentOrganizationId()` (usuário autenticado). **Do webhook não dá para reutilizar.**
- O `MockLexEngine` só emite `query`/`financingMessage` (texto cru), sem nenhum slot qualificado.
- Conclusão: hoje o bot conversa e faz handoff, mas **não qualifica**; o vendedor recebe o lead sem dados novos.

### Outros problemas encontrados no caminho
1. **Handoff não gera timeline.** `BotSessionService.markHandoff:42` seta `pipelineStage = WAITING_ATTENDANCE` direto. Regra do projeto: toda mudança de stage gera atividade/`LeadStageHistory`. `PipelineService.moveLead:53` faz isso, mas usa `SecurityUtils.getCurrentUser()` (sem usuário no webhook). `LeadStageHistory.movedBy` é nullable (`LeadStageHistory.kt:21`), então dá para criar variante "sistema".
2. **Fora do horário o lead sai do bot na primeira mensagem.** `HandoffEvaluator.kt:18` retorna `BUSINESS_HOURS` sempre que `!isOpenNow()`, antes de qualquer qualificação. Horário fixo 9–18 todos os dias, sem fim de semana nem config por organização (`BusinessHoursService.kt:174-176`).
3. **Lead duplicado quebra o inbound.** `LeadIngestionService.kt:43-56` cria uma segunda linha `Lead` com o mesmo telefone (`isDuplicate=true`); `BotService.handleIncoming:41` usa `findByOrganizationIdAndPhoneNumber(...): Optional<Lead>` (`LeadRepository.kt:14-17`) → com 2 linhas lança `IncorrectResultSizeDataAccessException`. Não verifiquei se há constraint única que impeça o caso; se não houver, o bug é real.
4. **Exceção no router contamina a transação.** `BotService.handleIncoming` é `@Transactional` e captura `Exception` (`BotService.kt:80-82`), mas `ChatbotRouter.handle` também é `@Transactional` (propagação REQUIRED): uma exceção que atravessa o proxy marca a transação como rollback-only, e o commit externo lança `UnexpectedRollbackException` (provável; confirmar com teste). Consequência: webhook 500 → Meta reenvia → mensagens duplicadas. **`AwsLexEngine` nunca pode lançar.**
5. **Lex não é medido.** `AiTokenTracker.kt:70` ignora saídas com `tokenUsage.total <= 0`, e o Lex não retorna tokens. Uso de Lex fica invisível; `AiQuotaGuard` só protege Bedrock (`ChatbotRouter.kt:72`).
6. `BotSession` só sai de `ACTIVE` por handoff; não há job para `COMPLETED/ABANDONED` (fora do escopo, só registrar).

---

## 2. Design da qualificação

### Slots (7) e mapeamento para o Lead
Nome **não** é slot: já vem do perfil WhatsApp (`BotService` recebe `contactName`). Menos uma pergunta, e `AMAZON.FirstName` em pt_BR é incerto (confirmar).

| Slot Lex | Tipo | Obrigatório p/ "qualificado" | Grava em | Regra |
|---|---|---|---|---|
| `vehicle` | texto livre (ver risco R1) | sim | `Lead.vehicleInterest` | preencher se vazio; se o lead veio do marketplace com valor diferente, manter o original e anexar em `notes` ("Interesse informado no chat: …") |
| `paymentType` | custom `PaymentType` | sim | `Lead.interestType` | À vista→`CASH_PURCHASE`, Financiamento→`FINANCING`, Troca→`TRADE_IN` |
| `timeline` | custom `PurchaseTimeline` | sim | `Lead.purchaseForecast` (texto, ex. "30 dias") + `Lead.urgency` derivada | IMEDIATO→`alta`, 30_DIAS→`media`, 90_DIAS_OU_MAIS→`baixa` (valores que o frontend já usa: `LeadFormDialog.tsx:156-158`) |
| `budget` | `AMAZON.Number` | não | `Lead.budget` | validar no Spring (>0, teto sanidade); "50 mil" pode não ser interpretado pelo Lex (confirmar) |
| `downPayment` | `AMAZON.Number` | não (só se Financiamento) | `Lead.notes` ("Entrada: R$ X") | não existe coluna; decisão: nota agora, coluna `down_payment` só se o CRM precisar filtrar |
| `hasTradeIn` | custom Sim/Não | não | `Lead.notes` | se sim, pergunta `tradeInVehicle` |
| `tradeInVehicle` | texto livre | não | `Lead.notes` ("Troca: …") | |

Regra de escrita: **nunca sobrescrever** dado preenchido por humano/marketplace com valor vazio; `notes` é append com prefixo "[Bot]".

### Onde implementar
Novo `chatbot/qualification/LeadQualificationService` (sem `SecurityUtils`, recebe `Lead` já carregado, roda dentro da transação do router): `apply(lead, slotValues)` + `isQualified(slotValues)`. Chamado em `ChatbotRouter.handle` depois de `applyOutput` (linha 47). Slots entram em `BotOutput.slotValues` com chaves canônicas (`vehicle`, `paymentType`, `timeline`, …) — o `AwsLexEngine` normaliza nomes e valores resolvidos (`PaymentType`→enum).

### Transições e handoff
- `TALKING_TO_BOT` → `WAITING_ATTENDANCE` quando: (a) qualificado (3 obrigatórios preenchidos) → novo `HandoffReason.QUALIFIED`; (b) `REQUEST_HUMAN`; (c) `READY_TO_BUY`; (d) `fallbackCount >= 3`; (e) manual. Coluna `handoff_reason` é `VARCHAR(80)` (`V13__bot_session.sql:10`), então novo valor de enum **não exige migration**.
- Handoff deve criar `LeadStageHistory` (movedBy=null, "sistema") e a atividade da timeline: criar `PipelineService.moveLeadAsSystem(leadId, orgId, toStage)` sem `SecurityUtils` e usá-lo em `markHandoff`.
- Mensagem final do bot no handoff resume o que foi capturado ("Anotei: …. Um consultor vai te chamar") — gerada pelo Spring, não pelo Lex.
- **Fora do horário:** o bot continua qualificando 24h (é o valor do produto). `BUSINESS_HOURS` só entra quando o handoff seria acionado (qualificado/humano/comprar): o lead vai para `WAITING_ATTENDANCE` e a mensagem diz "retornamos às 9h". Mudar `HandoffEvaluator` para: `if (reason != null && !isOpenNow()) → mantém reason mas texto de horário`. Horário por organização fica como melhoria (hoje hardcoded).
- **Duplicado:** resolver o lead do inbound com `findFirst…OrderByCreatedAtAsc` preferindo o não arquivado (corrige o bug 3 também); a qualificação grava no lead da sessão; se `isDuplicate`, acrescentar `notes` "[Bot] Lead duplicado" e **não** criar nova sessão se já existe `ACTIVE` para o telefone (regra de produto atual: duplicado ainda dispara mensagem, mantida).

---

## 3. Bot Lex V2

### Decisão de fulfillment: **Spring controla, sem Lambda**
Desligar o fulfillment code hook e o dialog code hook. O Lex faz NLU + elicitação de slots + confirmação; quando todos os slots do intent estão preenchidos devolve `intent.state = ReadyForFulfillment` (sem Lambda, o Lex só entrega o estado ao cliente). O Spring lê slots/estado e decide resposta final, gravação no Lead e handoff. Vantagens: menos peças na AWS (sem Lambda, sem IAM extra), lógica testável em Kotlin, versionada no mesmo repo. Custo: respostas "de negócio" (resumo, horário) são escritas no Spring.

### Intents (nomes idênticos ao enum `BotIntent` → mapeamento direto)
`GREETING`, `VEHICLE_INQUIRY` (slots `vehicle`, `paymentType`, `timeline`, `budget`), `FINANCING_SIMULATION` (`paymentType` fixo=Financiamento, `downPayment`, `budget`), `SCHEDULE_TEST_DRIVE` (`AMAZON.Date`, `AMAZON.Time` → `notes`), `BUSINESS_HOURS`, `REQUEST_HUMAN`, `READY_TO_BUY`, `AMAZON.FallbackIntent`→`FALLBACK`. Nome de intent desconhecido → `FALLBACK`. Trocar `KeywordIntentResolver` por exemplos de fala (≥10 por intent, base nas regras do `KeywordIntentResolver.kt:167-175`).

Lex mantém slots por intent; o Spring é a fonte de verdade entre intents: `BotSessionService.applyOutput` já faz merge (`putAll`). Ao retomar, passar slots conhecidos em `sessionState.intent.slots` para o Lex não repetir perguntas.

### Slot types
- Custom `PaymentType` (restrict values): `A_VISTA` [à vista, dinheiro, avista], `FINANCIAMENTO` [financiar, parcelar, parcelado, parcelas], `TROCA` [trocar, dar meu carro na troca, usado na troca].
- Custom `PurchaseTimeline` (restrict): `IMEDIATO` [hoje, agora, essa semana, o quanto antes], `30_DIAS` [esse mês, mês que vem, em um mês], `90_DIAS_OU_MAIS` [sem pressa, só pesquisando, mais pra frente].
- Custom `YesNo`. Built-ins: `AMAZON.Number`, `AMAZON.Date`, `AMAZON.Time` (confirmar disponibilidade em pt_BR).
- **R1 — `vehicle`/`tradeInVehicle` em texto livre:** o Lex V2 não tem slot "qualquer texto" garantido em pt_BR (`AMAZON.FreeFormInput` — confirmar). Spike T0 decide: (a) FreeFormInput se suportado; (b) custom type `ORIGINAL_VALUE` (sem restrição, NLU irregular); (c) fallback robusto: o Spring guarda `_elicit=vehicle` em `BotSession.slotValues` e, na mensagem seguinte, usa o texto cru do usuário (`inputTranscript`) como valor. Para leads vindos de marketplace, `vehicleInterest` já existe: o bot **confirma** em vez de perguntar.

### Prompts (pt_BR, tom do `CLAUDE.md`: informal-profissional)
- `vehicle`: "Qual carro você está procurando? Pode dizer marca e modelo."
- `paymentType`: "Como você pretende pagar: à vista, financiamento ou com troca?"
- `timeline`: "Pra quando você pensa em fechar: essa semana, nos próximos 30 dias ou sem pressa?"
- `budget`: "Qual valor você tem em mente para o carro?"
- `downPayment`: "Quanto pretende dar de entrada?"
- `hasTradeIn`: "Tem um carro para dar na troca?" / `tradeInVehicle`: "Qual é o carro (marca, modelo e ano)?"
- Fallback: reaproveitar as frases do `MockLexEngine.kt:23-30` como resposta do Lex para manter o comportamento já validado.
- Idle session TTL do bot: ≥ 1800 s (padrão 300 s) para alinhar com o cache Redis de 30 min; ainda assim re-hidratar slots do `BotSession` se o Lex expirou.

### Locale / versão / alias
`pt_BR` (`ChatbotConfig.Lex.localeId`, já default). Fluxo: editar `DRAFT` → build → versão numerada → alias (`staging` e `prod`) apontando para a versão. O app usa **alias**, nunca `TestBotAlias`, para trocar versão sem redeploy do backend. Disponibilidade do Lex V2 e de pt_BR por região e latência a partir da região do backend: confirmar (afeta também LGPD/residência de dados).

### `AwsLexEngine` — chamada
```
client.recognizeText {
  botId, botAliasId, localeId,
  sessionId = input.sessionId.toString()        // BotSession.id (UUID, dentro do limite 2–100 chars)
  text = input.message
  sessionState {
    sessionAttributes = { orgId, leadId }       // só IDs; nenhum dado pessoal
    intent { name, slots }                      // somente ao re-hidratar sessão expirada
  }
}
```
- Mapeamento da resposta: `messages[]`→`message` (concatenar `content`); `sessionState.intent.name`→`BotIntent`; `sessionState.intent.slots[*].value.interpretedValue` (ou `resolvedValues[0]`)→`slotValues`; `interpretations[0].nluConfidence` baixa (< limiar configurável) → `FALLBACK`.
- Resiliência: client com `apiCallTimeout` ~3 s e 1 retry; qualquer `SdkException`/timeout → log + métrica + devolve a saída do `TemplateEngine` (mensagem neutra + `FALLBACK`). Nunca lançar (ver 1.4). Após 3 falhas seguidas o `fallbackCount` já provoca handoff humano.
- Bean: `chatbot/config/ChatbotAwsConfig` com `@ConditionalOnProperty(app.chatbot.engine.mode=aws)` criando `LexRuntimeV2Client` e `BedrockRuntimeClient` (região de `app.aws.region`, hoje `AWS_REGION`, credenciais `DefaultCredentialsProvider` como em `SqsConfig`). Em AWS (EC2/ECS) usa role da instância, sem chaves no env.
- Medição: registrar 1 "request de Lex" por chamada (nova métrica de uso `LEX_TEXT_REQUESTS` em `UsageLog`, ou contador Micrometer na primeira versão) — sem isso não há como cobrar nem auditar custo.

### IAM mínimo (role do backend)
- `lex:RecognizeText` em `arn:aws:lex:<região>:<conta>:bot-alias/<BOT_ID>/<ALIAS_ID>`.
- (Bedrock, se ativo) `bedrock:InvokeModel` no ARN do modelo escolhido.
- Role de serviço do próprio Lex (criada pelo IaC): `AWSServiceRoleForLexV2Bots` / permissões para logs no CloudWatch, se habilitar conversation logs. Não ligar logs com texto do cliente sem decisão de LGPD.

### Custo (estimativa a confirmar)
Lex V2 texto ≈ US$ 0,00075 por request (confirmar na página de preços e no free tier). Uma qualificação típica ≈ 6–10 turnos → ≈ US$ 0,005–0,008 por lead; ~1.000 leads/mês ≈ US$ 5–8. Irrelevante frente ao orçamento de teste de US$ 100; o custo relevante continua sendo compute/banco.

---

## 4. Bedrock (opcional, fase posterior)

- **Quando entra:** só quando o Lex devolve `FALLBACK` **e** o tier é `LEX_BEDROCK_FALLBACK` **e** `AiQuotaGuard.canUseTokens` permite (já é a regra em `ChatbotRouter.route:70-76`). Não entra no caminho feliz da qualificação.
- **Uso:** API Converse com system prompt fechado ("assistente de concessionária; responda só sobre veículos/atendimento; nunca prometa preço, prazo ou aprovação de crédito; peça para falar com consultor quando não souber"). `maxTokens` baixo (~200). Contexto de estoque simples, sem vetor: top N veículos da organização filtrados por `vehicleInterest` via `VehicleRepository`, injetados como texto — evita RAG/OpenSearch (custo e complexidade).
- **Segurança:** texto do cliente só no turno `user` (nunca concatenado no system); sem tool use; Bedrock **não** escreve no Lead nem decide handoff; Bedrock Guardrails (tópicos negados: promessas financeiras/preço, PII) — confirmar suporte ao idioma/modelo.
- **Intent da saída:** devolver `VEHICLE_INQUIRY` (como o mock, `MockBedrockEngine.kt:141-142`), **não** `FALLBACK`, senão `BotOutput.fallback` incrementa `fallbackCount` e força handoff mesmo com resposta válida.
- **Tracking:** `TokenUsage` a partir de `response.usage()` (input/output reais); o `AiTokenTracker` e a quota mensal (`plan.maxAiTokensPerMonth`) já cobrem. `requestedTokens=1` no guard (`ChatbotRouter.kt:72`) subestima; passar a estimativa do prompt.
- Modelo e custo por token dependem do modelo habilitado na conta (acesso ao modelo precisa ser liberado no console) — decisão adiada para depois do Lex estar em produção.

---

## 5. IaC e configuração

**Recomendação: CloudFormation (YAML único) em `infra/lex/leadflow-bot.yaml`**, implantado com `aws cloudformation deploy` num job do GitHub Actions. Motivo: `AWS::Lex::Bot` aceita locales, intents, slots e slot types inline, e existem `AWS::Lex::BotVersion` e `AWS::Lex::BotAlias` (confirmar cobertura). No Terraform existem `aws_lexv2models_bot/_bot_locale/_intent/_slot/_slot_type/_bot_version`, mas não conheço recurso de alias (confirmar); como o projeto ainda não tem Terraform, não vale introduzir outra ferramenta só para isso. Alternativa se o time preferir CDK: `CfnBot` equivalente.
Outputs do stack: `BotId`, `BotAliasId` → vão para parameter store/secret e viram `AWS_LEX_BOT_ID`/`AWS_LEX_BOT_ALIAS_ID`.

Config (já existe em `application.yaml:104-114`; só falta ligar):
```
CHATBOT_ENGINE_MODE=aws
APP_AWS_ENABLED=true
AWS_REGION=<região do bot>
AWS_LEX_BOT_ID=...  AWS_LEX_BOT_ALIAS_ID=...  AWS_LEX_LOCALE_ID=pt_BR
AWS_BEDROCK_MODEL_ID=...        # só se Bedrock ativo
```
Novas props opcionais: `app.chatbot.aws.lex.min-confidence` (default 0.4), `app.chatbot.aws.lex.timeout-ms` (default 3000). Atenção: `app.aws.enabled=true` também liga `SqsConfig` (exige filas); para testar só o Lex, o `ChatbotAwsConfig` deve depender apenas de `app.chatbot.engine.mode=aws`.

---

## 6. Plano de implementação (tarefas atômicas)

Ordem pensada para entregar valor sem AWS primeiro (T1–T4 funcionam em modo mock e já corrigem bugs reais).

| # | Tarefa | Horas |
|---|---|---|
| T1 | `LeadQualificationService` (slots→Lead, sem SecurityUtils, nunca sobrescreve) + ligar no `ChatbotRouter` + `MockLexEngine` emitindo slots canônicos + testes | 4 |
| T2 | `PipelineService.moveLeadAsSystem` + `LeadStageHistory`/atividade no handoff; `HandoffReason.QUALIFIED`; regra de qualificação completa | 3 |
| T3 | Corrigir lookup de lead duplicado em `BotService` (e teste com 2 linhas mesmo telefone) | 1 |
| T4 | `HandoffEvaluator`: bot qualifica fora do horário, `BUSINESS_HOURS` só quando há handoff; ajustar testes | 2 |
| T0 | **Spike Lex (antes de T5):** criar bot descartável no console pt_BR; validar `FreeFormInput`, `AMAZON.Number` ("50 mil"), `ReadyForFulfillment` sem Lambda, latência | 2–3 |
| T5 | `infra/lex/leadflow-bot.yaml` (intents, slot types, prompts, versão, alias staging/prod) | 5 |
| T6 | `ChatbotAwsConfig` (clients, timeout) + `AwsLexEngine` real + mapeamento + fallback sem exceção | 5 |
| T7 | Métrica de uso Lex (`LEX_TEXT_REQUESTS`) + visibilidade no painel Chatbot existente | 2 |
| T8 | `scripts/validate-lex-integration.sh` (aws `lexv2-runtime recognize-text`, no estilo de `validate-meta-integration.sh`) + runbook em `docs/RUNBOOK-CHATBOT.md` | 2 |
| T9 | `AwsBedrockEngine` real (Converse + estoque + guardrails + tracking real) — **opcional/adiável** | 6 |
| | **Total até Lex em produção (T1–T8 + T0)** | **≈ 26–27 h** |

### Testes
- Unitários: `LeadQualificationService` (mapeamento, não-sobrescrita, derivação urgency, parse de budget), `HandoffEvaluator` (matriz horário × intent × qualificado), duplicado em `BotService`.
- `AwsLexEngineTest` com `LexRuntimeV2Client` mockado (Mockito): resposta completa, intent desconhecido→FALLBACK, confiança baixa→FALLBACK, `SdkException`/timeout→saída TemplateEngine sem exceção.
- Contract tests: fixtures JSON de respostas reais do `RecognizeText` (gravadas no spike T0) desserializadas e passadas pelo mapper — protege contra mudança de formato.
- `ChatbotRouterTest` estendido: conversa de 4 turnos mock→lead qualificado→`WAITING_ATTENDANCE` + `LeadStageHistory` criado.
- Teste de transação: exceção no engine não causa `UnexpectedRollbackException` no `BotService`.
- E2E manual: `validate-lex-integration.sh` contra o alias `staging` antes de apontar `prod`.

### Riscos
| Risco | Mitigação |
|---|---|
| R1: slot de texto livre fraco em pt_BR | spike T0 + fallback via `inputTranscript` no Spring |
| R2: NLU pt_BR errando "50 mil", gírias | validação no Spring, `min-confidence`, fallback humano após 3 erros |
| R3: dependência externa da Meta (template `welcome_lead` aprovado, número verificado) bloqueia teste ponta a ponta com WhatsApp real | testar o chatbot via `MockMetaApiClient` + script direto no Lex; Meta é trilha paralela |
| R4: LGPD — texto de conversa enviado ao Lex/Bedrock e logs | não ligar conversation logs sem decisão; mandar só IDs em `sessionAttributes`; confirmar região |
| R5: custo/quotas do Lex e Bedrock descontrolados | métrica T7, alarmes no AWS Budgets, Bedrock só atrás da quota existente |
| R6: bug de duplicado/transação (itens 1.3 e 1.4) já existe hoje em mock | T3 e teste de transação independem da AWS — corrigir primeiro |
| R7: preços/suporte regional citados aqui estão desatualizados | marcados "confirmar"; checar antes de decidir região |
