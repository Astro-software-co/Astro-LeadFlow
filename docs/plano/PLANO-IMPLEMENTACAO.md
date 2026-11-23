# Plano de implementação — maturidade para acesso às APIs de marketplace

> Versão 2 · data-base 2026-11-23 · Branch de trabalho: `claude/marketplace-integrations-ynztxc` (parte de `develop`)
> Leitor: um agente de código de alta capacidade (Claude CLI) trabalhando **dentro do repositório**.
> Complementos: `docs/plano/RESUMO-EXECUTIVO.md` (visão gerencial) e `docs/plano/FORMULARIO-WEBMOTORS.md` (rascunho das respostas).
>
> **O que mudou na v2:** (1) nova **Fase 4 — Modelo Card + Interesses** (decisões do Ruy de 2026-11-23); (2) **remoção do legado Webmotors** entra já na Fase 1; (3) **proteção de segredos** (credenciais, tokens, API keys) e **máscara de logs** ganham tarefas explícitas (Fases 1 e 6); (4) quatro **decisões ainda em discussão** estão isoladas na seção 1.4 com o padrão assumido.

---

## 0. Prompt mestre (cole no CLI)

```
Leia na ordem: CLAUDE.md, openspec/config.yaml, openspec/STATUS.md e docs/plano/PLANO-IMPLEMENTACAO.md.
Execute UMA fase por vez, na ordem do plano. Antes de codar cada fase, crie a change do OpenSpec
correspondente (/openspec-propose) usando o texto da seção "Change OpenSpec" da fase. Siga as
"Regras de trabalho" (seção 2) e o "Definition of Done" (seção 3). Ao terminar cada tarefa: rode os
testes indicados, faça um commit pequeno e descritivo e atualize a checklist da fase neste arquivo.
Ao terminar a fase: rode a suíte completa, atualize openspec/STATUS.md e PARE para eu revisar.
Não altere .github/workflows/ nem infra/aws/ (CI/CD e AWS estão sob responsabilidade do Ruy).
Se uma premissa da seção 1 ou um item marcado [VERIFICAR] não bater com o código, pare e me pergunte.
Itens da seção 1.4 (decisões em discussão) usam o padrão assumido; não os trate como definitivos.
```

Para fases isoladas: `Execute somente a Fase N do plano docs/plano/PLANO-IMPLEMENTACAO.md, seguindo as regras da seção 2.`

---

## 1. Contexto, decisões e premissas

### 1.1 Estado de partida (verificado em 2026-11-23)

- `develop` está em `64c7d63` e contém: CI/CD em standby (`.github/workflows/{ci,build-images,deploy,security}.yml`), OpenSpec (`openspec/`), chatbot (mock), billing Abacate Pay (mock), RBAC `ASTRO_ADMIN | MANAGER | SELLER`, migrations **V1–V18**.
- A minha branch contém o módulo **`integrations/webmotors`** (polling da API de Consulta de Leads, caixa de entrada de propostas, aceite/recusa, credenciais cifradas, endpoints, 47 testes), mergeado com a `develop` de 2026-11-18 (`4825abc`). **Ainda não foi mergeado com a `develop` atual** — isso é a Fase 0.
- No merge com a `develop` atual: único conflito textual em `backend/src/main/resources/application.yaml`; único erro de compilação em `CrmLeadIntakeAdapter` (usa `LeadRepository.findByOrganizationIdAndPhoneNumber`, removido na `develop`). Ambos verificados num worktree descartável.
- A migration do módulo está como `V5__webmotors_leads_polling.sql` e **colide** com `V5__saas_plan_offers.sql`. Deve virar a próxima versão livre (hoje **`V19`**; confirmar com `ls backend/src/main/resources/db/migration`).
- O arquivo `v1__init_schema.sql` (minúsculo) **já foi removido** da `develop`; manter só `V*__*.sql`.

### 1.2 Fonte de verdade para a Webmotors

**A collection Postman "API Lead" + a pesquisa (`docs/integrations/webmotors.md`, `docs/INTEGRACOES-MARKETPLACES-META.md`) valem mais do que o código Webmotors que já existia no `ingestion`** (confirmado pelo Ruy). O código antigo assume push por webhook, campos `NomeComprador/TelefoneComprador`, endpoint `/leads/v1/{id}` e autenticação em dois passos; nada disso aparece na collection, que mostra: *password grant* (Basic `client_id:secret` + usuário/senha do integrador), polling em `GET /lead/v1/leads`, aceite/recusa em `PATCH /lead/v1/leads/{id}/aceite`, headers `client_id` e `access_token`.

### 1.3 Decisões tomadas

| # | Decisão | Origem |
|---|---|---|
| D1 | **Polling é o único caminho Webmotors.** O código legado Webmotors do `ingestion` é **removido** (Fase 1, T1.6), não apenas desativado. Mercado Livre continua no `ingestion`. | Ruy, 2026-11-23 |
| D2 | **Modelo Card + Interesses** (Fase 4): um card ativo por cliente (telefone) com vários interesses (anúncios) por card; reabertura em até 3 meses se o interesse for o mesmo anúncio; senão, novo card. Substitui a regra `isDuplicate`. Regras completas na Fase 4. | Ruy, 2026-11-23 |
| D3 | **Conversa salva de forma generalista** enquanto o card está aberto; só no **fechamento** (arquivado ou concluído) a conversa é etiquetada com o interesse mais recente. Retenção do **conteúdo** da conversa: **3 meses após o fechamento**. | Ruy, 2026-11-23 |
| D4 | **Status por interesse:** `CONSIDERANDO` (em aberto), `DESCARTADO` (cliente não quer mais), `ESCOLHIDO` (cliente decidiu comprar). | Ruy, 2026-11-23 |
| D5 | **Novo interesse em card ativo:** sem mensagem automática ao cliente; notifica o vendedor se o card já foi assumido; notifica o dono da loja (`MANAGER`) se o card está em aberto sem atendimento ou com o bot. | Ruy, 2026-11-23 |
| D6 | **Vendedor responsável é um por card.** Sem dados legados a migrar (não há leads reais em produção). | Ruy, 2026-11-23 |
| D7 | **Hexagonal fora de escopo.** Mantém-se arquitetura em camadas por módulo, com portas só onde já existem (`WebmotorsApi`, `CrmLeadIntake`, `MarketplaceLeadFetcher`). | Ruy |
| D8 | **Mercado Livre:** sem módulo novo agora; só o hardening do `ingestion` (Fase 7). A integração real depende da pesquisa em andamento do Ruy. | Ruy |
| D9 | **Banco no deploy inicial:** Postgres no Compose com backup para S3 (staging); RDS só em produção. | Assumida; Ruy confirma na change `aws-deploy-activation` |

### 1.4 Decisões EM DISCUSSÃO (padrão assumido até o Ruy fechar)

| # | Tema | Padrão assumido no plano | Onde impacta |
|---|---|---|---|
| P1 | **Lead sem telefone** | Fica **pendente** (visível no CRM, sem mensagem), nunca descartado | T7.4, T4.x (identidade), T5.x (UI) |
| P2 | **Aceite na Webmotors** | `AUTO` por padrão, `MANUAL` configurável por loja | T1.4, Fase 5 |
| P3 | **Mensagem de boas-vindas** | Ligada por padrão, desligável por loja; base legal a validar com o jurídico | T1.4, T4.5 |
| P4 | **LGPD** | Registro de solicitações + anonimização + página pública + e-mail de privacidade; sem portal self-service | Fases 2 e 3 |
| P5 | **Dados pessoais de card fechado após os 3 meses** | Só o **conteúdo** das conversas é apagado; nome/telefone/e-mail do card permanecem (base de clientes da loja). Revisar com o jurídico. | T4.9 |
| P6 | **Cliente responde no WhatsApp a um card arquivado (sem anúncio no contexto)** | Dentro dos 3 meses, **reabre o card fechado mais recente** (a resposta continua a conversa); depois dos 3 meses, card novo | T4.7 |
| P7 | **Concluir card com vários interesses** | O interesse atual é marcado `ESCOLHIDO` automaticamente; o gerente pode trocar com um clique | T4.6 |
| P8 | **Estágio ao reabrir** | `TALKING_TO_BOT` (o bot revalida os dados), mantendo o vendedor responsável | T4.7 |

---

## 2. Regras de trabalho (valem para todas as fases)

1. **Convenções do projeto** (`openspec/config.yaml`, `CLAUDE.md`): pacote canônico `com.astro.leadflow.shared.modules.*`; **uma classe por arquivo, sem DTO inline**; Flyway sequencial (`V*__*.sql`, nunca editar migration já mergeada); PT-BR; BRL; Lucide; sem dark mode por padrão; responsivo em 375/768/1024/1440.
2. **Multi-tenancy:** `organizationId` sempre via `SecurityUtils.getCurrentOrganizationId()` (JWT). Toda query de dado de loja filtra por organização. Chaves do React Query começam com `orgId`; logout limpa o cache.
3. **RBAC real do código:** `ASTRO_ADMIN` (interno, sem organização), `MANAGER`, `SELLER`. **Não existe role `ADMIN`.** `SecurityConfig` usa `hasRole("DEALERSHIP")` para as APIs de concessionária e `hasRole("MANAGER")` para DELETE e referral [VERIFICAR como `JwtAuthenticationFilter` mapeia MANAGER/SELLER → `DEALERSHIP`].
4. **Segredos e PII:** nunca em log, nunca em teste/fixture reais, nunca em `localStorage`. Telefone/e-mail só mascarados em log (T1.5). **Credenciais e tokens de terceiros ficam cifrados em repouso; API keys ficam como hash** (Fase 6). *Nota de vocabulário:* "anonimizar" vale para dado pessoal de titular (Fase 2); segredos técnicos são **cifrados** (reversíveis, pois precisamos usá-los) ou **hasheados** (só comparados).
5. **Ambiente de build/test:**
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   export LC_ALL=C.UTF-8 LANG=C.UTF-8      # sem isso o Kotlin falha em testes com acento no nome
   cd backend && ./gradlew test --no-daemon
   cd ../frontend && npm ci && npm run lint && npm run test:coverage && npm run build
   ```
   `LeadflowApplicationTests` precisa de Postgres e Redis em `localhost` (ver `backend/compose.yaml` ou os `services:` de `ci.yml`). Sem eles, só essa classe falha — nunca "corrigir" mudando o teste.
6. **Testes primeiro quando houver regra de negócio.** Cobrir caminho feliz, erro, isolamento entre organizações, papel não autorizado e concorrência quando aplicável.
7. **Commits pequenos** (`feat|fix|refactor|test|docs(escopo): ...`), um por tarefa. Sem `--no-verify`, sem force-push, sem reescrever histórico publicado.
8. **Não tocar** em `.github/workflows/` nem `infra/aws/`. Se a fase exigir mudança lá, descrever em "Pedido ao Ruy" no fim da fase.
9. **Dúvida de contrato Webmotors?** Não inventar. Marcar `// TODO(homologacao): ...`, registrar na tabela da Fase 10 e seguir com o comportamento defensivo (campos anuláveis, `ignoreUnknown`).
10. **Migrations e H2:** o H2 dos testes `@DataJpaTest` não suporta índice parcial nem `pg_advisory_*`. Regras que dependam disso devem ser testadas **também** em Postgres real (Testcontainers ou o Postgres do CI) e ter um fallback em código.

---

## 3. Definition of Done (por tarefa e por fase)

- [ ] Compila (`./gradlew compileKotlin compileTestKotlin`) e a suíte do módulo tocado passa.
- [ ] Suíte completa: backend sem regressões (exceto `LeadflowApplicationTests` se não houver Postgres/Redis) e frontend `lint`, `test:coverage`, `build` verdes.
- [ ] Uma classe por arquivo e nenhum import legado `com.astro.leadflow.modules.*`.
- [ ] Migrations aplicam em Postgres 16 limpo na ordem `V1…Vn` e batem com o que o Hibernate espera (`spring.jpa.hibernate.ddl-auto=validate` contra o banco migrado).
- [ ] Documentação atualizada (`docs/`, `openspec/STATUS.md`) e a change do OpenSpec com tarefas marcadas.
- [ ] Sem segredo/PII em código, logs ou fixtures.

---

## 4. Mapa de dependências

```
F0 Consolidar ──► F1 Alinhar Webmotors + remover legado + máscara de logs
        │                  │
        │                  ├──► F4 Card + Interesses ──► F5 UI (Webmotors + Interesses)
        │                  └──► F7 Hardening ingestão (ML + sem telefone)
        ├──► F2 LGPD ──► F3 Dados institucionais/formulário ──► [ENVIAR FORMULÁRIO]
        ├──► F6 Segredos (credenciais, tokens, API keys)
        └──► F8 Observabilidade ──► F9 Pré-deploy (código) ──► F10 Homologação*
* F10 só depois que a Webmotors liberar o acesso.
```

**Caminho crítico para abrir a solicitação:** F0 → F2 → F3 (+ insumos que só o Ruy tem). F1, F4, F5 e F6 aumentam a credibilidade técnica e **não bloqueiam** o envio.

Estimativas (ordem de grandeza, 1 desenvolvedor sênior ou agente): F0 0,5d · F1 2,5d · F2 3–4d · F3 1d (+ insumos) · **F4 5–7d** · F5 3–4d · F6 3d · F7 3d · F8 1d · F9 2d · F10 1–2d.

---

## FASE 0 — Consolidar a branch com a `develop` atual

**Change OpenSpec:** `webmotors-leads-polling` — *Why:* a Webmotors só oferece API de consulta (pull); o código Webmotors do `ingestion` assume push e não segue a collection. *What:* módulo `integrations/webmotors` (login password grant, polling, caixa de entrada, aceite/recusa, credenciais cifradas), remoção do fluxo legado e UI de gestão. *Impact:* `integrations/webmotors`, `ingestion`, `saas` (credenciais), migrations, frontend (aba Integrações).

**Objetivo:** branch compilando e testada sobre a `develop` mais recente.

**Passos**
1. `git fetch origin && git checkout claude/marketplace-integrations-ynztxc && git merge origin/develop`. (Merge, **não** rebase: a branch já foi publicada.)
2. Conflito em `application.yaml`: ficar com a versão da `develop` e reanexar no fim o bloco `integrations.webmotors`. O arquivo usa **CRLF**: manter CRLF no bloco. Conferir que não há chave `integrations:` duplicada.
3. Renomear a migration para a próxima versão livre (hoje `V19`). Atualizar referências em `docs/integrations/webmotors.md` (seções 7 e 8).
4. Corrigir o único erro de compilação: em `CrmLeadIntakeAdapter.kt`, trocar `findByOrganizationIdAndPhoneNumber` por `findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc` **apenas para compilar**; a T1.3 reescreve o adaptador.
5. Suíte completa (seção 2, regra 5). Esperado: tudo verde.
6. Validar a cadeia `V1…V19` em Postgres limpo e `ddl-auto=validate`.
7. `git push -u origin claude/marketplace-integrations-ynztxc`.

**Aceite:** compila (main e test); 47 testes `integrations.webmotors` verdes; nenhuma versão de migration duplicada.

- [ ] F0 concluída

---

## FASE 1 — Alinhar o módulo Webmotors, remover o legado e mascarar logs

**Objetivo:** o módulo respeita as regras do projeto; o legado Webmotors sai; nenhum dado sensível vaza em log (nem para o banco, já que logs de ERROR são persistidos).

### T1.1 — Uma classe por arquivo, sem DTO inline
Dividir (mesmo pacote/nomes, **zero mudança de comportamento**, 47 testes sem alteração além de imports):

| Arquivo | Dividir em |
|---|---|
| `infra/WebmotorsDtos.kt` (10 tipos) | `WmTokenResponse`, `WmLeadPage`, `WmLeadItem`, `WmLoja`, `WmAnuncio`, `WmVendedor`, `WmEnvelope`, `WmKeyValue`, `WmDecisionRequest`, `WmStatusRequest` (em `infra/dto/`) |
| `infra/WebmotorsApi.kt` | `WebmotorsApi`, `WmCredentials`, `WebmotorsApiException` |
| `api/WebmotorsController.kt` | controller + `WebmotorsAccountRequest`, `RefuseProposalRequest` (`api/requests/`), `ProposalResponse` (`api/responses/`) + `ProposalResponseMapper` |
| `service/WebmotorsAccountService.kt` | + `AccountCommand`, `AccountView`, `AccountRuntime` |
| `service/WebmotorsInboxService.kt` | + `InboxEntry` |
| `service/WebmotorsLeadSyncService.kt` | + `SyncReport` |
| `service/WebmotorsContactExtractor.kt` | + `WmContact` |
| `service/CrmLeadIntake.kt` | `CrmLeadIntake`, `IncomingLead`, `IntakeResult` |
| `domain/WebmotorsAccount.kt` | + `AcceptMode` |
| `domain/WebmotorsLeadInbox.kt` | + `InboxStatus` |
| `domain/WebmotorsRepositories.kt` | `WebmotorsAccountRepository`, `WebmotorsLeadInboxRepository` |

### T1.2 — RBAC real
- `WebmotorsController`: `hasRole('MANAGER')` no lugar de `hasAnyRole('ADMIN','MANAGER')`.
- `SecurityConfig`: `.requestMatchers("/api/v1/integrations/**").hasRole("MANAGER")` **antes** do bloco `DEALERSHIP` [VERIFICAR ordem e mapeamento de roles].
- `ASTRO_ADMIN` não tem organização: `SecurityUtils.getCurrentOrganizationId()` deve resultar em 403 limpo (não 500) — cobrir em teste.
- Testes: `SELLER` → 403; `MANAGER` de outra organização → 404 na proposta alheia; sem token → 401.

### T1.3 — Entrada no CRM por uma única porta, já preparada para "interesse"
**Problema:** `CrmLeadIntakeAdapter` cria lead por conta própria e contradiz o fluxo oficial de ingestão.

**Passos**
1. Evoluir a porta `CrmLeadIntake` para receber o **comando de entrada completo**, já pensando na Fase 4: `LeadIntakeCommand(organizationId, name, phone?, email, origin(marketplace), externalEventId, listingId?, listingUrl?, vehicleSnapshot(brand, model, version, year, price, plate)?, contactType?, message?, receivedAt, sendWelcome)`. `IncomingLead` é substituído por esse comando.
2. Extrair de `LeadIngestionService` a criação de lead (normalização, criação, cota via `usageLimitService`, `sendTemplate` com tratamento de `QuotaExceededException`/`ExternalServiceException`) para um método público reutilizável `LeadIngestionService.ingestResolved(organization, command)`. `ingestFromWebhook`/`processFromQueue` passam a chamá-lo. **Não mudar o comportamento dessas entradas** (rodar `LeadIngestionServiceTest` [VERIFICAR nome] antes e depois).
3. [VERIFICAR] contagem de uso: o ramo de duplicado chama `incrementUsage` explicitamente e o ramo normal não; conferir se `MessageSenderService.sendTemplate` já incrementa e se há dupla contagem ou ausência de contagem. Documentar; corrigir só se for claramente bug, com teste.
4. Reescrever `CrmLeadIntakeAdapter` para delegar a esse método e **remover** o `TransactionTemplate` e o anexo de notas ao lead existente. **Nesta fase o comportamento de duplicidade continua o atual da `develop`** (`isDuplicate=true` + mensagem); a Fase 4 substitui a regra **dentro deste único ponto**.
5. Testes: lead novo; telefone repetido (segundo lead com `isDuplicate=true` e mensagem enviada); cota excedida (lead criado sem mensagem); falha da Meta não desfaz o lead.

### T1.4 — Padrões de conta (P2, P3 — em discussão)
- `AcceptMode` default `AUTO`; `welcomeMessageEnabled` default `true` (entidade e migration, ainda não publicada em produção — editar a migration do módulo só se ela **não** foi mergeada na `develop`; caso contrário, nova migration `ALTER`).
- Atualizar testes e `docs/integrations/webmotors.md` (7.1). **Se P2/P3 mudarem, só os defaults e a UI mudam.**

### T1.5 — Máscara de PII e segredos em log (nada sensível em log, nada sensível no banco)
**Contexto:** `logback-spring.xml` tem um `DatabaseErrorAppender` que grava logs de **ERROR em tabela** (`application_error`); portanto, PII em log de erro vira dado persistido e precisa entrar também no escopo de anonimização/retenção (Fase 2 e 4).

1. `shared/util/PiiMasker.kt`: `maskPhone("+5511987654321") -> "+55******4321"`, `maskEmail("m***@dominio.com")`, `maskToken(x) -> "***"+últimos4`, com testes.
2. **Rede de segurança global:** `PiiMaskingConverter` (Logback `ClassicConverter`/`MessageConverter`) registrado em `logback-spring.xml` e aplicado **ao encoder do console e ao `DatabaseErrorAppender`**, mascarando por regex: telefones BR (E.164 ou com DDD), e-mails, e padrões `access_token|refresh_token|client_secret|authorization|api[-_]?key` seguidos de valor. Testes de unidade com mensagens reais e de **log capturado** (ex.: `OutputCaptureExtension`) provando que o valor não aparece.
3. **Parar de logar corpo/payload:** remover `body={}`/`response={}`/`payload={}` de `communication/infra/MetaClient.kt` (linhas ~80, 89), `communication/meta/RealMetaApiClient.kt` (~80, 86) e `ingestion/api/controller/MarketplaceWebhookController.kt` (log do payload no `catch`). Logar apenas IDs (leadId, eventId, correlationId) e códigos HTTP.
4. **Explícito nos pontos conhecidos** (usar `PiiMasker`): `ingestion/service/LeadIngestionService.kt` (~44, 83, 96, 112, 121), `communication/service/BotService.kt` (~43), `communication/infra/SqsConsumer.kt` (~23), `communication/meta/{Mock,Real}MetaApiClient.kt`, `iam/service/TwoFactorService.kt` (~48, 66, 68). [VERIFICAR com `grep -rnE "log\.\w+\(.*(phone|Phone|email|Email|toPhone|token|secret)"`; a lista pode ter mudado.]
5. **Exceções:** `e.message` pode carregar PII (ex.: `"Número de telefone inválido: $raw"` em `NormalizationService`, `"Lead ... sem telefone"`). Remover o valor cru das mensagens de exceção; manter só o motivo.
6. Teste de arquitetura (JUnit, varrendo `src/main`): falha se um `log.*` interpolar variável com nome `phone|email|token|secret|password` sem `PiiMasker`.

### T1.6 — Remover o código legado Webmotors (D1)
**Não** manter atrás de flag: nunca rodou contra a Webmotors real e o contrato não bate com a collection.

Remover (verificar com `git grep -n -i -E "WebmotorsLeadFetcher|WebmotorsAuthService|WebmotorsWebhookRequest|receiveWebmotors|wmAccessToken|wmClientId|wmClientSecret|wmIntegratorEmail|wmTokenExpiresAt|wmCnpj|marketplace\.webmotors"`):

| Arquivo / ponto | Ação |
|---|---|
| `ingestion/api/controller/MarketplaceWebhookController.kt` | remover o método `receiveWebmotors` e imports; manter `receiveMercadoLivre` |
| `ingestion/api/requests/WebmotorsWebhookRequest.kt` | apagar |
| `ingestion/service/marketplace/WebmotorsLeadFetcher.kt` | apagar |
| `ingestion/service/marketplace/WebmotorsAuthService.kt` | apagar |
| `src/test/.../WebmotorsWebhookControllerTest.kt`, `.../WebmotorsLeadFetcherTest.kt` | apagar |
| `saas/domain/MarketplaceCredential.kt` | remover `wmClientId`, `wmClientSecret`, `wmAccessToken`, `wmTokenExpiresAt`, `wmIntegratorEmail`, `wmCnpj` |
| Nova migration | `ALTER TABLE marketplace_credentials DROP COLUMN wm_*` (6 colunas) **após** conferir que nenhum código/frontend lê essas colunas e que não há linhas `WEBMOTORS` em uso. Manter `marketplace_type='WEBMOTORS'` no `CHECK` e o enum `MarketplaceType.WEBMOTORS` (usado em cota, `marketplace_configs` e origem do lead) |
| `application.yaml`, `infra/prod/.env.example`, `docker-compose*` | remover `marketplace.webmotors.*` (`base-url`, `oauth-url`, `client-id`, `client-secret`) e `WEBMOTORS_BASE_URL/OAUTH_URL/CLIENT_ID/CLIENT_SECRET` |
| `MarketplaceLeadConsumer` | garantir que continua subindo com a lista de fetchers contendo só o do Mercado Livre (teste de contexto) |
| Docs/specs | `docs/INTEGRACOES-MARKETPLACES-META.md` (gap #3 e tabela 1), `openspec/specs/lead-ingestion/spec.md` (via change: "Webmotors entra por polling"), `openspec/STATUS.md` (regra #3 resolvida) |

`Organization.cnpj` **permanece** (usado em onboarding/identificação; verificar antes de qualquer mudança).

### T1.7 — Verificação final da fase
Suíte completa; `grep` mostrando: nenhum `hasAnyRole('ADMIN'`, nenhum import legado, nenhuma referência ao legado Webmotors, nenhum `log.*` com PII sem máscara; migration aplicada em Postgres limpo.

- [ ] T1.1 · [ ] T1.2 · [ ] T1.3 · [ ] T1.4 · [ ] T1.5 · [ ] T1.6 · [ ] T1.7

---

## FASE 2 — LGPD: solicitações de titulares e anonimização (bloqueia o formulário)

**Change OpenSpec:** `lgpd-data-subject-requests` — *Why:* o formulário de acesso à Webmotors pergunta o canal oficial e o SLA para anonimização; hoje só existe `DELETE /leads/{id}` (remoção física, restrita a `MANAGER`) e ele **provavelmente falha para leads vindos de marketplace**: `marketplace_webhook_events.lead_id` referencia `leads(id)` **sem** `ON DELETE` [VERIFICAR em V3]. *What:* tabela de solicitações com prazo, serviço de anonimização que cobre todas as tabelas com PII, endpoints para `MANAGER`, bloqueio de contato a leads anonimizados, expurgo de payloads brutos. *Impact:* `crm`, `communication`, `chatbot`, `ingestion`, `monitoring`, `integrations/webmotors`, migrations, frontend.

> **Nota jurídica:** a Astro é **operadora**; a concessionária é **controladora** e responde ao titular. Textos e prazos são tecnicamente consistentes com a LGPD (art. 18 e 19) mas **devem ser revisados por advogado** antes de publicar.

### Mapa de PII (reconfirmar lendo cada entidade)
| Onde | PII | Tratamento |
|---|---|---|
| `leads` | `name`, `phone_number` (NOT NULL), `email`, `notes` | `name`→"Titular anonimizado"; `phone_number`→`ANON-<8 hex>` (≤ 20 chars); `email`/`notes`→null; manter estágio/interesse/valor (métrica); novo `anonymized_at` |
| `chat_messages.content` | conteúdo livre | → `[mensagem removida]` |
| `bot_sessions.slot_values` | JSON com nome/veículo/orçamento | → `{}` |
| `marketplace_webhook_events.raw_payload` | payload bruto | → `{}` para eventos do titular; expurgo por retenção (T2.5) |
| `webmotors_lead_inbox` | `message`, `raw_payload` | → null |
| **`application_error`** | mensagens de ERROR podem conter PII (ver T1.5) | varrer por telefone/e-mail do titular e redigir; **retenção de 90 dias já existe** (`MonitoringConfig.errorRetentionDays`) — documentar |
| `webhook_event_log`, `lead_stage_history` | [VERIFICAR] `errorMessage` | só se houver PII comprovada |
| `lead_interests`, `lead_interest_events` (Fase 4) | snapshot de veículo/anúncio (não é PII); `last_contact` metadados | manter; **atualizar este mapa ao concluir a Fase 4** |
| Outros leads com o mesmo telefone/e-mail | idem | anonimizar **todos** os leads da organização com o mesmo identificador |

### T2.0 — Corrigir o `DELETE /leads/{id}` para leads de marketplace
Migration `ALTER TABLE marketplace_webhook_events` recriando a FK `lead_id` com `ON DELETE SET NULL`; teste que deleta um lead com evento associado (hoje deve falhar → depois passa).

### T2.1 — Modelo de dados (migration)
- `ALTER TABLE leads ADD COLUMN anonymized_at TIMESTAMP;`
- `data_subject_requests (id UUID PK, organization_id UUID NOT NULL FK, type VARCHAR(30), status VARCHAR(30), subject_hash VARCHAR(64) NOT NULL, subject_hint VARCHAR(40), channel VARCHAR(30), received_at TIMESTAMP NOT NULL, due_at TIMESTAMP NOT NULL, completed_at TIMESTAMP, handled_by UUID, result_summary TEXT, created_at TIMESTAMP NOT NULL)`.
  - `type`: `ANONYMIZATION | ACCESS | CORRECTION | DELETION`; `status`: `RECEIVED | IN_PROGRESS | COMPLETED | REJECTED`.
  - `subject_hash` = HMAC-SHA256 do identificador normalizado com `privacy.hash-secret`; `subject_hint` = identificador mascarado. **Nunca** guardar o identificador em claro.
  - `due_at = received_at + privacy.sla-days` (default 15).
- Entidade, enums e repositório (uma classe por arquivo).

### T2.2 — `LeadAnonymizationService.anonymize(organizationId, identifier): AnonymizationReport` (`@Transactional`)
1. Normalizar o identificador (telefone via `NormalizationService.normalizePhone`; e-mail minúsculo).
2. Localizar **todos** os leads da organização com aquele telefone/e-mail.
3. Aplicar o mapa de PII, tudo na mesma transação.
4. `AnonymizationReport(leadsAnonymized, messagesRedacted, sessionsCleared, eventsCleared, inboxCleared, errorsRedacted)` — **sem** PII.
5. Idempotente. Blindagens: `MessageSenderService` e o bot **não** enviam a lead com `anonymized_at != null`; o inbound do WhatsApp nunca reassocia mensagem a lead anonimizado.
6. Testes: isolamento por organização; vários leads do mesmo telefone; idempotência; relatório sem PII; envio bloqueado; identificador inexistente → solicitação `COMPLETED` com "nenhum dado encontrado".

### T2.3 — Endpoints (`MANAGER`; matcher `/api/v1/privacy/**` em `SecurityConfig`)
`POST /api/v1/privacy/requests`; `POST /api/v1/privacy/requests/{id}/execute`; `POST /api/v1/privacy/requests/{id}/reject`; `GET /api/v1/privacy/requests?status=` (com `overdue`). `DELETE /leads/{id}` segue existindo como remoção operacional (documentar a diferença). Atualizar `docs/API.md` e Insomnia; testes 401/403/404/isolamento.

### T2.4 — Frontend: aba "Privacidade" em Configurações (`MANAGER`)
Padrão service/hook/componente (ver Fase 5): lista com prazo e selo "Atrasada"; "Nova solicitação"; "Executar anonimização" com confirmação **irreversível**. Testes de componente e hook (chaves `orgId`).

### T2.5 — Retenção de payloads brutos
Job `@Scheduled` (`SchedulingConfig` já existe) que limpa `raw_payload` de `marketplace_webhook_events` e `webmotors_lead_inbox` além de `privacy.retention.webhook-payload-days` (default 90). Desligado em dev; ligado em `prod`. Teste com relógio fixo.

### T2.6 — Runbook
`docs/RUNBOOK-PRIVACIDADE.md`: receber pedido (canal, validação de identidade **pela controladora**), executar, SLA, o que **não** é apagado (agregados/métricas), backups (a anonimização não retroage a cópias de segurança; descrever a janela).

- [ ] T2.0 · [ ] T2.1 · [ ] T2.2 · [ ] T2.3 · [ ] T2.4 · [ ] T2.5 · [ ] T2.6

---

## FASE 3 — Dados institucionais e pacote do formulário

**Change OpenSpec:** `marketplace-access-readiness` — *Why:* o formulário da Webmotors exige canal de suporte, site, logo, CNPJ, política de privacidade e canal LGPD que a plataforma ainda não expõe. *What:* páginas legais públicas, configuração institucional por ambiente, rascunho das respostas. *Impact:* frontend (landing/rotas públicas), `docs/`.

- **T3.1** `frontend/src/config/company.ts` lendo `VITE_COMPANY_NAME`, `VITE_COMPANY_CNPJ`, `VITE_SUPPORT_EMAIL`, `VITE_SUPPORT_PHONE`, `VITE_PRIVACY_EMAIL`, `VITE_SITE_URL`; `.env.example` com placeholders (sem valores reais no repositório).
- **T3.2** Rotas públicas `/privacidade` e `/termos` em `LandingLayout` (`App.tsx`, `React.lazy`/`Suspense`); `LandingFooter.tsx`: trocar os `href: '#'` (~linhas 17 e 100). Conteúdo da política (rascunho em `FORMULARIO-WEBMOTORS.md`, seção B): papéis operadora × controladora, dados, finalidades, bases legais, compartilhamento (Meta/WhatsApp, Abacate Pay, AWS), retenção (inclui os **3 meses** de conversa de cards fechados — D3), direitos e canal, prazo, encarregado [PREENCHER]. Marcar **"revisar com advogado"** no PR.
- **T3.3** Logo PNG em `frontend/public/brand/` (hoje só há `favicon.ico`, `og-image.png`, `placeholder.svg`); arquivo vem do Ruy.
- **T3.4** Completar `FORMULARIO-WEBMOTORS.md` com os dados reais e fixar o texto final.

**Insumos do Ruy (bloqueiam o envio):** razão social e CNPJ; **domínio** e e-mails (`suporte@`, `privacidade@`) com telefone; nome e contato do solicitante; **nome do app** no Portal; logo PNG; regiões de atuação; CNPJ de concessionária(s) piloto; SLA a declarar.

- [ ] T3.1 · [ ] T3.2 · [ ] T3.3 · [ ] T3.4

---

## FASE 4 — Modelo Card + Interesses (decisões D2–D6)

**Change OpenSpec:** `lead-interests-model` — *Why:* o produto precisa rastrear o interesse do cliente por anúncio sem criar um card por carro e sem perder a visibilidade quando o cliente consulta vários veículos; o `isDuplicate` é um remendo que duplica cards e mensagens. *What:* entidade de interesse por card com status próprio, regra de card ativo por telefone, reabertura em até 3 meses para o mesmo anúncio, etiquetagem da conversa no fechamento e retenção do conteúdo. *Impact:* `crm`, `ingestion`, `communication`, `chatbot`, `integrations/webmotors`, métricas, frontend (Kanban e detalhe do lead), `openspec/specs/lead-ingestion` (requisito "Duplicados" substituído) e nova spec `lead-interests`.

### 4.1 Regras (decididas — implementar exatamente assim)

| # | Regra |
|---|---|
| R1 | Identidade do cliente = **telefone normalizado dentro da organização**. |
| R2 | Existe no máximo **1 card ativo** por `(organização, telefone)`. **Ativo** = estágio fora de `COMPLETED`/`ARCHIVED`. |
| R3 | Entrada de lead (marketplace/scraper) com card ativo: **adiciona ou atualiza um interesse** no mesmo card; **não** cria card nem envia boas-vindas. |
| R4 | Cada interesse é identificado por `(marketplace, listingId)`. Contato repetido no mesmo anúncio **não** duplica o interesse: incrementa `contact_count` e atualiza `last_contact_at`. Eventos repetidos (mesmo `externalEventId`) são idempotentes. |
| R5 | **Interesse atual** = entre os interesses `CONSIDERANDO`, o de maior `last_contact_at` (desempate: o criado por último). Se não houver `CONSIDERANDO`, o de maior `last_contact_at` entre todos. O card guarda o ponteiro `current_interest_id`, recalculado a cada mudança. |
| R6 | Status por interesse: `CONSIDERANDO` (padrão), `DESCARTADO`, `ESCOLHIDO`. Alterável por `MANAGER`/vendedor do card. |
| R7 | A **conversa é salva de forma generalista** (mensagens do card sem etiqueta por anúncio) enquanto o card está aberto. |
| R8 | **No fechamento** (`COMPLETED` ou `ARCHIVED`): grava `closed_at`; define `closing_interest_id` (o `ESCOLHIDO` se houver; senão o atual, R5); **etiqueta todas as mensagens ainda sem etiqueta** com esse interesse. |
| R9 | Card **fechado há ≤ 90 dias** + nova entrada para o **mesmo anúncio** (`marketplace`, `listingId`) → **reabre o mesmo card** (R10). Anúncio **diferente**, ou card fechado há > 90 dias → **card novo**. |
| R10 | **Reabertura:** limpa `closed_at`, `reopened_count++`, estágio `TALKING_TO_BOT` (P8), mantém o vendedor responsável, registra atividade "Reaberto", **consolida** os dados do card e dispara a **revalidação** pelo bot (4.4). Envia a mensagem de reengajamento/boas-vindas (como card novo). |
| R11 | **Novo interesse em card ativo** (R3) notifica: o **vendedor** se o card já foi assumido (`assignedTo != null`); os **`MANAGER`** da organização se o card está **em aberto sem atendimento ou com o bot** (`TALKING_TO_BOT`/`WAITING_ATTENDANCE` sem responsável). |
| R12 | **Retenção:** 90 dias após `closed_at`, o **conteúdo** das conversas (mensagens, `slot_values` do bot, `notes`) é removido; `leads`/interesses (metadados e métricas) permanecem. A retenção é por card, não reabre depois de executada. LGPD (Fase 2) prevalece sobre a retenção. |
| R13 | Concluir (`COMPLETED`) com interesses: se nenhum estiver `ESCOLHIDO`, o **atual** é marcado `ESCOLHIDO` automaticamente (P7); `leads.vehicle_id` recebe o veículo do `ESCOLHIDO` (alimenta a receita do dashboard). |
| R14 | `isDuplicate` deixa de ser escrito como `true` para leads novos; coluna e campo de API permanecem por compatibilidade até uma limpeza posterior. |
| R15 | Resposta do cliente no WhatsApp: se há card ativo, vai nele; se não, e há card fechado há ≤ 90 dias, **reabre o mais recente** (P6); senão cria card novo (comportamento atual do `BotService`). |

### 4.2 Modelo de dados (migration nova, próxima versão livre)
- `lead_interests (id UUID PK, lead_id UUID NOT NULL FK leads ON DELETE CASCADE, organization_id UUID NOT NULL, marketplace_type VARCHAR(30) NOT NULL, listing_id VARCHAR(100), listing_url TEXT, vehicle_id UUID FK vehicles ON DELETE SET NULL, vehicle_brand, vehicle_model, vehicle_version, vehicle_year INT, vehicle_price NUMERIC(12,2), vehicle_plate, status VARCHAR(20) NOT NULL DEFAULT 'CONSIDERANDO', contact_count INT NOT NULL DEFAULT 1, last_contact_type VARCHAR(40), first_contact_at, last_contact_at, created_at, updated_at, UNIQUE (lead_id, marketplace_type, listing_id))` (índice por `(organization_id, marketplace_type, listing_id)` para a busca de reabertura).
- `lead_interest_events (id UUID PK, interest_id UUID NOT NULL FK ON DELETE CASCADE, marketplace_type, external_event_id VARCHAR(100) NOT NULL, received_at, UNIQUE (marketplace_type, external_event_id, interest_id))` — idempotência (R4).
- `ALTER TABLE leads ADD COLUMN current_interest_id UUID, closing_interest_id UUID, closed_at TIMESTAMP, reopened_count INT NOT NULL DEFAULT 0, conversation_purged_at TIMESTAMP;` (FKs `ON DELETE SET NULL`).
- `ALTER TABLE chat_messages ADD COLUMN tagged_interest_id UUID` (FK `ON DELETE SET NULL`).
- **Índice parcial de unicidade (Postgres):** `CREATE UNIQUE INDEX ux_leads_one_active_per_phone ON leads (organization_id, phone_number) WHERE pipeline_stage NOT IN ('COMPLETED','ARCHIVED') AND is_duplicate = false AND anonymized_at IS NULL;` (a cláusula `is_duplicate = false` mantém fora do índice eventuais duplicados legados). Em código, tratar a violação como "alguém criou o card agora": refazer a resolução e adicionar o interesse (retry único). Testar em Postgres real (regra 10).

### 4.3 Backend (tarefas)
- **T4.1** Migration + entidades (`LeadInterest`, `LeadInterestEvent`, `InterestStatus`) + repositórios; ajustar `Lead` e `ChatMessage`. [VERIFICAR] as demais mudanças de `Lead` (já tem `isDuplicate`, `vehicle`).
- **T4.2** `LeadIntakeService` (ponto único chamado por `CrmLeadIntakeAdapter`, scraper e consumer do ML): resolve `(org, phone)` → **R3 / R9 / novo card**, aplicando idempotência (R4), `welcome` só em card novo ou reaberto, `usageLimitService` coerente, índice parcial como rede de segurança. Substitui a lógica de `isDuplicate` de `LeadIngestionService`.
- **T4.3** `LeadInterestService`: `addOrTouch`, `changeStatus`, `recomputeCurrent` (R5); validações de papel e organização; atividade de linha do tempo "Interesse adicionado" / "Interesse atualizado" [VERIFICAR modelo de atividades em `crm`].
- **T4.4** Revalidação do bot na reabertura (R10): ao reabrir, mover os `slotValues` coletados para um estado "a revalidar" para os campos **mutáveis** (telefone, e-mail, valor de entrada, urgência, tipo de interesse/financiamento, troca) e manter os **estáveis** (nome); o bot confirma cada campo ("Seu e-mail continua X?") e consolida com o que já havia. [VERIFICAR nomes canônicos dos slots em `docs/CHATBOT-LEX-PLANO.md` e `BotSessionService`.]
- **T4.5** Mensagem etiquetada com o anúncio (decisão 1 do Ruy): o template de boas-vindas/reengajamento inclui o **veículo do interesse atual**. Requer `sendTemplate` com `components` (variáveis) e um template aprovado que as aceite — ver change `meta-whatsapp-real` (2.2); **fallback** para o template sem variáveis enquanto a Meta não estiver real.
- **T4.6** `LeadClosureService` (R8, R13): hook chamado pelos pontos que movem para `COMPLETED`/`ARCHIVED` (`PipelineService.moveLead`, `LeadService.archive`) — grava `closed_at`, define `closing_interest_id`, etiqueta mensagens (`UPDATE chat_messages SET tagged_interest_id=… WHERE lead_id=… AND tagged_interest_id IS NULL`), aplica o `ESCOLHIDO` automático e o `vehicle_id`.
- **T4.7** `LeadReopenService` (R9, R10, R15): reabre o card; ajusta `BotService.handleIncoming` para resolver o card (ativo → fechado recente → novo). Teste de concorrência: dois eventos simultâneos do mesmo telefone resultam em **um** card.
- **T4.8** `LeadNotificationService` (R11): **não existe infraestrutura de notificação** hoje — só as preferências `notifyViaEmail`/`notifyViaPush` em `User`. Entregar o mínimo: tabela `notifications` (`user_id`, `type`, `lead_id`, `interest_id`, `created_at`, `read_at`) + `GET /api/v1/notifications` + `PATCH .../read`, e **e-mail** quando `notifyViaEmail` (via `EmailService`; MailHog em dev, SES em prod — change `email-ses-prod`). Push fora do escopo [VERIFICAR se o frontend já tem sino/lista no `Header`].
- **T4.9** `ConversationRetentionJob` (R12, P5): diário; para cards com `closed_at < now − privacy.retention.conversation-days` (default 90) e `conversation_purged_at IS NULL`: apagar `chat_messages` do card, limpar `bot_sessions.slot_values`, limpar `notes`, gravar `conversation_purged_at`. Idempotente, com relógio injetável nos testes; ligado em `prod`.
- **T4.10** Métricas/dashboard: receita por `leads.vehicle_id` do `ESCOLHIDO` (queries `sumRevenueByOrganizationAndPeriod`, `countSalesByOrganizationAndPeriod` permanecem corretas via R13; adicionar teste); leads por marketplace passam a contar **entradas** (eventos), não cards — decidir o que o dashboard mostra e documentar.
- **T4.11** Atualizar o mapa de anonimização da Fase 2 (`lead_interests`, `lead_interest_events`, `notifications`) e o `LeadAnonymizationService` (testes).
- **T4.12** Specs e docs: reescrever o requisito "Duplicados" em `openspec/specs/lead-ingestion/spec.md` (via change), criar `openspec/specs/lead-interests/spec.md` com os cenários R1–R15, atualizar `CLAUDE.md` (regra "Duplicate leads…"), `openspec/config.yaml` e `openspec/STATUS.md` (regras #1 e #2), `docs/ARCHITECTURE.md`.

### 4.4 Testes obrigatórios da fase
Sem card → novo; card ativo + anúncio novo → interesse adicionado, sem mensagem, notificação certa (vendedor × `MANAGER`); mesmo anúncio de novo → contador, sem duplicar; evento repetido → idempotente; fechar → etiqueta, `closing_interest`, `ESCOLHIDO` automático; reabrir dentro de 90 dias no mesmo anúncio → mesmo card, revalidação; anúncio diferente → card novo; > 90 dias → novo; retenção apaga conteúdo e mantém métrica; concorrência (2 threads, mesmo telefone) → 1 card; isolamento entre organizações; anonimização cobre as tabelas novas.

- [ ] T4.1 · [ ] T4.2 · [ ] T4.3 · [ ] T4.4 · [ ] T4.5 · [ ] T4.6 · [ ] T4.7 · [ ] T4.8 · [ ] T4.9 · [ ] T4.10 · [ ] T4.11 · [ ] T4.12

---

## FASE 5 — UI de gestão (Webmotors) e UI de interesses

**Padrão a seguir (ler antes):** `frontend/src/components/settings/ReferralTab.tsx`, `services/referrals.service.ts`, `hooks/api/useReferrals.ts`, `types/api/referrals.ts`, `test/ReferralTab.test.tsx`, `pages/Configuracoes.tsx` e `settings.helpers.ts` (`settingsTabs`).

### 5.A Webmotors
- **T5.1** `types/api/webmotors.ts` espelhando exatamente os DTOs do backend; `services/webmotors.service.ts`; `hooks/api/useWebmotors.ts` (chaves `[orgId, 'webmotors', ...]`; invalidar propostas após aceitar/recusar/sincronizar).
- **T5.2** Componentes em `components/settings/webmotors/`: `WebmotorsAccountForm` (**nunca** preencher segredos com o valor salvo; mostrar `clientIdHint`; "em branco mantém"; "Testar conexão"), `WebmotorsSyncStatus` (último sync, erro, "Sincronizar agora", trata sync em andamento), `WebmotorsProposalsTable` (filtro por status; cartões no mobile; Aceitar/Recusar com motivos de `GET /refusal-reasons`; selo `AWAITING_CONTACT` com tentativas; link para o lead). Aba `integracoes` em `settingsTabs` (somente `MANAGER`).
- **T5.3** Testes de serviço, hook, componente e `ConfiguracoesPage.test.tsx`.

### 5.B Interesses (Kanban e detalhe do lead)
- **T5.4** Tipos/serviço/hook: `LeadInterest`, `InterestStatus` (`CONSIDERANDO | DESCARTADO | ESCOLHIDO`), mudança de status; **sem** mexer no contrato existente do lead além de campos novos opcionais.
- **T5.5** Kanban/lista: chip "+N interesses" e veículo do **interesse atual**; badge de **reaberto**; filtro opcional por marketplace. Detalhe do lead: painel "Interesses" (veículo, marketplace, contatos, último contato, selo **Atual**, seletor de status com cores), link "Conversa anterior" para card fechado dentro da retenção, e aviso "Conversa removida por retenção" depois dela.
- **T5.6** Notificações: sino/lista no `Header` [VERIFICAR o que existe] consumindo `GET /api/v1/notifications`; texto PT-BR: "Novo interesse de {cliente} em {veículo}".
- **T5.7** Testes (hooks com chaves `orgId`; componentes; regressão de Kanban/Leads) e verificação em 375/768/1024/1440 (Playwright ou descrição no PR).

- [ ] T5.1 · [ ] T5.2 · [ ] T5.3 · [ ] T5.4 · [ ] T5.5 · [ ] T5.6 · [ ] T5.7

---

## FASE 6 — Segredos em repouso: credenciais, tokens e API keys

**Change OpenSpec:** `secrets-at-rest` — *Why:* credenciais de marketplace, token do WhatsApp e API keys estão em texto puro no banco. *What:* cifrar o que precisa ser reutilizado (AES-256-GCM) e hashear o que só é comparado (API keys), com migração gradual sem indisponibilidade e fail-fast em produção.

**Inventário (verificado; reconfirmar):**

| Segredo | Onde | Tratamento |
|---|---|---|
| `ml_client_secret`, `ml_access_token`, `ml_refresh_token` | `marketplace_credentials` | **cifrar** |
| `wm_*` | `marketplace_credentials` | **removido na T1.6** |
| `whatsapp_token` | `organizations` | **cifrar** |
| `api_key` | `organizations` (`OrganizationRepository.findByApiKey`, `ApiKeyAuthFilter`) | **hashear** (SHA-256 com pepper); buscar por hash; mostrar a chave **uma única vez** na criação |
| Credenciais Webmotors por loja | `webmotors_accounts` | **já cifradas** (migrar para o `FieldCipher` compartilhado) |
| `ABACATEPAY_API_KEY`, `ABACATEPAY_WEBHOOK_SECRET`, `JWT_SECRET`, `META_APP_SECRET` | variáveis de ambiente / SSM | **não** ficam no banco; garantir que não aparecem em log nem em `/actuator` |

- **T6.1** Mover `SecretCipher` para `shared/security/FieldCipher` (mesma API e testes; chave `app.security.field-encryption-key`, com `integrations.webmotors.secrets-key` como alias de compatibilidade).
- **T6.2** `EncryptedStringConverter` (`AttributeConverter`) com prefixo `enc:v1:` e `keyId` para suportar **duas chaves** na rotação; **tolerante a texto puro** na leitura (valor sem prefixo = legado) e sempre cifra na escrita.
- **T6.3** Aplicar nos campos `ml_*` e `whatsapp_token` [VERIFICAR que nenhuma query filtra por esses campos]. Atenção a `MessageSenderService` e ao cliente Meta, que leem `organization.whatsappToken`.
- **T6.4** API keys: migration adiciona `api_key_hash` (+ `api_key_prefix` para exibição), job/comando one-shot que hasheia as existentes e **anula** a coluna `api_key` em texto; `ApiKeyAuthFilter` passa a buscar por hash em tempo constante; endpoint de criação/rotação devolve a chave em claro **uma vez**. Testes: chave válida/ inválida/ rotacionada; o texto puro não existe mais no banco.
- **T6.5** Comando/Job one-shot `reencrypt-legacy-credentials` (perfil `ops`, idempotente) para regravar registros legados cifrados.
- **T6.6** **Fail-fast em `prod`:** a aplicação não sobe sem `app.security.field-encryption-key` válida (32 bytes em Base64) nem sem `privacy.hash-secret`; nunca imprimir o valor. Teste de contexto.
- **T6.7** Garantir que segredos não vazam: serialização (`@JsonIgnore`/`toString` sem segredo em entidades e DTOs de credencial), `/actuator/env` e `/actuator/configprops` com `management.endpoint.*.show-values=never`, testes que capturam log e falham se aparecer um token de teste.
- **T6.8** Documentar geração e **rotação** da chave (`openssl rand -base64 32`; rotação = re-cifrar com a nova chave mantendo a antiga em leitura por um período) e guardar a chave no SSM Parameter Store (pedido ao Ruy).
- Testes: round trip; leitura de legado; escrita cifrada (verificar no banco o prefixo); `prod` sem chave não sobe; API key sem texto puro.

- [ ] T6.1 · [ ] T6.2 · [ ] T6.3 · [ ] T6.4 · [ ] T6.5 · [ ] T6.6 · [ ] T6.7 · [ ] T6.8

---

## FASE 7 — Hardening da ingestão (Mercado Livre e lead sem telefone)

Executa a change existente `openspec/changes/ingestion-hardening` (tarefas 1.x–4.x). **Não** criar módulo novo do Mercado Livre (D8).

- **T7.1 Entrada sem AWS** (1.1–1.2): com `app.aws.enabled=false`, `MarketplaceWebhookController` processa inline (hoje o `MarketplaceLeadConsumer` só existe com AWS). Extrair o corpo de `MarketplaceLeadConsumer.process` para um serviço reutilizável. Teste: webhook sem SQS cria o lead.
- **T7.2 Autenticidade do webhook** (2.1–2.3): remover o `return true` quando `marketplace.webhook-token` está vazio em `prod` (fail-fast no boot **e** 401 em runtime). Para o ML, validar HMAC **somente** após confirmar o formato; até lá, validar consultando o recurso (`/vis/leads/{id}`). Responder ao ML em ≤ 500 ms (200 e processamento assíncrono).
- **T7.3 Refresh serializado do token ML** (3.1–3.2): lock por credencial (`SELECT … FOR UPDATE` ou advisory lock do Postgres) em `MercadoLivreAuthService.refreshToken`; `invalid_grant` marca a credencial `REAUTH_REQUIRED` e expõe isso na UI/alerta.
- **T7.4 Lead sem telefone** (4.1) — **padrão P1, em discussão:** hoje `LeadNormalizationService.normalize` lança `IllegalArgumentException("Lead … sem telefone — descartado")` e o consumer relança → DLQ. Desenho: migration que torna `leads.phone_number` anulável + `contact_status` (`COMPLETE|PENDING_CONTACT`); como a **identidade do cliente é o telefone (R1)**, um card pendente **não tem identidade**: unicidade por `(marketplace, externalLeadId)` e **sem** reabertura/fusão até o contato ser completado; ao completar via `PATCH /leads/{id}/contact`, resolver pela regra R2/R9 (fundir com card ativo do mesmo telefone ou seguir como card). `MessageSenderService` pula lead sem telefone; selo "Contato pendente" no Kanban; dedupe ignora nulo; regressão do inbound WhatsApp. Para a Webmotors o equivalente já é `AWAITING_CONTACT` da caixa de entrada.
- **T7.5 Dados do anúncio do ML** (4.2): buscar marca/modelo/ano por `item_id` e preencher o **snapshot do interesse** (Fase 4) e `vehicleInterest`.

- [ ] T7.1 · [ ] T7.2 · [ ] T7.3 · [ ] T7.4 · [ ] T7.5

---

## FASE 8 — Observabilidade e operação das integrações

Seguir o padrão de `monitoring` (`WebhookEventLog`, `ApplicationError`) e `infra/monitoring/prometheus/leadflow-alerts.yml` (monitoramento, não CI/CD).

- **T8.1** Métricas Micrometer [VERIFICAR dependências]: `integrations_webmotors_sync_total{outcome}`, `integrations_webmotors_proposals{status}`, `integrations_webmotors_api_errors_total{status}`, duração do sync; `lead_interests_added_total`, `lead_cards_reopened_total`, `conversation_retention_purged_total`.
- **T8.2** Alertas: sync falhando > 15 min; propostas `PENDING` por mais de N minutos em conta `AUTO`; propostas `FAILED`; job de retenção sem rodar em 48 h.
- **T8.3** `docs/RUNBOOK-WEBMOTORS.md` (401 repetido, `AWAITING_CONTACT` crescente, lease preso, chave de cifra ausente, como forçar sync e reprocessar `FAILED`).
- **T8.4** Painel Astro Admin: contas Webmotors habilitadas/com erro [VERIFICAR módulo `admin`].

- [ ] T8.1 · [ ] T8.2 · [ ] T8.3 · [ ] T8.4

---

## FASE 9 — Pré-deploy (lado do código)

Coordenar com `aws-deploy-activation` (CI/CD e AWS são do Ruy).

- **T9.1** Subir com `SPRING_PROFILES_ACTIVE=prod`, `APP_AWS_ENABLED=false` e variáveis de teste; corrigir o que quebrar (risco conhecido em `infra/aws/README.md`: `SqsAutoConfiguration` não é excluído em `prod`).
- **T9.2** Variáveis novas em `infra/prod/.env.example` e `docker-compose.prod.yml`: `WEBMOTORS_ENABLED=false`, `WEBMOTORS_LEADS_AUTH_URL`, `WEBMOTORS_LEADS_API_URL`, `APP_SECURITY_FIELD_ENCRYPTION_KEY`, `PRIVACY_HASH_SECRET`, `PRIVACY_SLA_DAYS`, `PRIVACY_RETENTION_WEBHOOK_PAYLOAD_DAYS`, `PRIVACY_RETENTION_CONVERSATION_DAYS`, `VITE_*` institucionais. **Webmotors desligada em produção até a homologação.**
- **T9.3** E-mail em produção (`email-ses-prod`): `SesEmailService` no perfil `prod` — necessário para convites, canal de privacidade e **notificações de novos interesses** (T4.8).
- **T9.4** `scripts/smoke-staging.sh`: login, criação de lead, Kanban, `GET /actuator/health`, `GET /api/v1/integrations/webmotors/account` (404 sem conta), `/privacidade`.
- **T9.5** "Pedido ao Ruy" para o que for de `.github/` (rodar `ci.yml` com os novos testes; opcional `openspec validate --all`).

- [ ] T9.1 · [ ] T9.2 · [ ] T9.3 · [ ] T9.4 · [ ] T9.5

---

## FASE 10 — Homologação Webmotors (somente após o acesso)

Cada linha é uma **suposição** do módulo. Para cada uma: obter amostra real **sanitizada**, guardar em `backend/src/test/resources/webmotors/`, escrever teste de contrato e ajustar o ponto indicado. (O legado já foi removido na T1.6.)

| # | Suposição | Onde ajustar | Como confirmar |
|---|---|---|---|
| H1 | Login `grant_type=password` com Basic `client_id:secret`, resposta 200/201 com `access_token`/`expires_in` | `WebmotorsHttpClient.token` | Chamada real em homologação |
| H2 | Headers dos dados: `client_id` + `access_token` | `WebmotorsHttpClient.authed` | idem |
| H3 | Parâmetro de paginação (`pagina`) e semântica de `paginaAtual/totalPaginas` | `WebmotorsProperties.pageParam`, `WebmotorsLeadSyncService` | Listagem com > 1 página |
| H4 | Formato do contato no detalhe e se o telefone vem mascarado antes do aceite | `WebmotorsContactExtractor` | `GET /lead/v1/leads/{id}` antes/depois do aceite |
| H5 | Aceitar não envia `codigo_motivo` | `WebmotorsHttpClient.accept` | `PATCH .../aceite` real |
| H6 | Significado de `tipoLead` (1, 2, `subTipoLead`) e `id_canal` | `GET /tipos`, `GET /canais` | Resposta real → `typeDescription` |
| H7 | Envelope de motivos (`Sucesso/Retorno/Mensagens`) | `WmEnvelope`, `refusalReasons` | `GET /motivos-recusa` |
| H8 | IDs numéricos como texto; datas como texto | `WmLeadItem` | Amostra real |
| H9 | Rate limit e intervalo mínimo de polling | `WebmotorsProperties.pollIntervalMs` | Combinar com a Webmotors |
| H10 | `https` nos endpoints de dados e URLs de produção | `application.yaml` | Documentação de produção |
| H11 | `loja.id` casa com `store_id` | `WebmotorsLeadSyncService` | Amostra real |
| H12 | Significado comercial de "aceitar" (P2) e penalidade por aceitar sem atender | decisão de produto | Reunião com a Webmotors |
| H13 | O `anuncio.id` é estável e serve de `listingId` do interesse (R4/R9) | `IncomingLead` → `LeadIntakeCommand` | Amostras de dois contatos do mesmo anúncio |

Depois: ligar `WEBMOTORS_ENABLED=true` em staging e produção; arquivar as changes (`/openspec-archive-change`).

- [ ] F10 concluída

---

## 5. Trilhas paralelas já existentes no OpenSpec (não duplicar)

| Change | Estado | Bloqueio | Relação com este plano |
|---|---|---|---|
| `aws-deploy-activation` | standby | Conta AWS (Ruy) | F9 coordena o lado do código |
| `email-ses-prod` | pendente | Domínio + conta AWS | T9.3; necessário para privacidade, convites e notificações (T4.8) |
| `meta-whatsapp-real` | pendente | Verificação de negócio, número, token, template | T4.5 (template com o veículo) e P3 dependem dele; custo/classificação ainda em aberto |
| `lex-real-engine` | pendente | Conta AWS | T4.4 usa os slots canônicos do chatbot |
| `abacatepay-sandbox-validation` | pendente | Conta/chave sandbox | Regras #6–#8 do STATUS seguem abertas |
| `ingestion-hardening` | pendente | Webmotors/ML | Executada na F7 |

---

## 6. Ações do Ruy (fora do código)

1. **Webmotors:** registrar o app no Portal do Desenvolvedor, preencher o formulário (`FORMULARIO-WEBMOTORS.md`), pedir ambiente de homologação e confirmar H3–H13.
2. **Dados institucionais** da Fase 3.
3. **Mercado Livre:** app em developers, redirect URI, tópico `vis_leads`, conta vendedora Motors (pesquisa em andamento).
4. **Meta:** Business Portfolio/verificação, número dedicado, token permanente, template `welcome_lead` **com variável do veículo** (T4.5).
5. **AWS/CI/CD:** conta, Budget, OIDC, EC2/SSM, **SSM Parameter Store para as chaves** (T6.8), SES, domínio/DNS.
6. **Jurídico:** política de privacidade, base legal do contato inicial por WhatsApp, papéis operadora × controladora, SLA declarado, e **retenção de dados pessoais de cards fechados** (P5).
7. **Fechar as decisões P1–P8** da seção 1.4.

---

## 7. Registro de riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Contrato real da Webmotors diferir da collection | Retrabalho em client/extractor | Tudo anulável + `ignoreUnknown`; F10 com testes de contrato |
| Condição de corrida cria 2 cards ativos do mesmo telefone | Cards duplicados, mensagem em dobro | Índice parcial único + retry + teste de concorrência em Postgres |
| Regra Card+Interesses mexer em fluxo central (ingestão, bot, Kanban, métricas) | Regressão ampla | Fase 4 com testes de regressão, porta única de entrada (T1.3), feature flag `leads.interests.enabled` para ligar/desligar [recomendado] |
| Anonimização esquecer uma tabela com PII (inclui `application_error` e tabelas novas) | Descumprimento LGPD | Mapa de PII revisado + teste que varre colunas por nome |
| Logs de ERROR persistidos no banco com PII | Vazamento persistente | T1.5 (máscara global antes do appender de banco) + retenção de 90 dias |
| Perda da chave de cifra | Credenciais irrecuperáveis | Cofre (SSM), `keyId` no prefixo, rotação documentada |
| `phone_number` anulável (T7.4) quebrar dedupe/Kanban/inbound | Regressão em fluxo central | Migração incremental e testes de regressão do inbound WhatsApp |
| Retenção apagar conteúdo que o jurídico exige manter | Perda de prova/relacionamento | Retenção configurável por propriedade e revisão jurídica (P5) |
| Migrations fora de ordem entre branches | Falha de deploy | Conferir a próxima versão livre no momento de cada merge |
| Meta/AWS ainda em mock | "Funciona" sem prova real | Registrar como não validado em `openspec/STATUS.md` |

---

## 8. Checklist final de aceitação do plano

- [ ] Branch consolidada com a `develop`, suíte verde, migrations em ordem (F0)
- [ ] Webmotors alinhado, **legado removido**, **logs sem PII/segredos** (F1)
- [ ] LGPD: solicitações + anonimização + retenção + runbook (F2)
- [ ] Páginas legais e rascunho do formulário prontos; insumos do Ruy recebidos (F3)
- [ ] Modelo Card + Interesses em operação com retenção e reabertura (F4)
- [ ] UI Webmotors e UI de interesses/notificações (F5)
- [ ] **Segredos cifrados/hasheados**, fail-fast em `prod` (F6)
- [ ] Ingestão ML endurecida e política de lead pendente aplicada (F7)
- [ ] Métricas, alertas e runbook das integrações (F8)
- [ ] `prod` sobe localmente; `.env.example` e smoke test prontos (F9)
- [ ] **Formulário enviado à Webmotors**
- [ ] Homologação concluída (F10)
