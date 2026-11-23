# Plano de implementação — maturidade para acesso às APIs de marketplace

> Data-base: 2026-11-23 · Branch de trabalho: `claude/marketplace-integrations-ynztxc` (parte de `develop`)
> Leitor: um agente de código de alta capacidade (Claude CLI) trabalhando **dentro do repositório**.
> Complementos: `docs/plano/RESUMO-EXECUTIVO.md` (visão gerencial) e `docs/plano/FORMULARIO-WEBMOTORS.md` (rascunho das respostas).

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
```

Para fases isoladas: `Execute somente a Fase N do plano docs/plano/PLANO-IMPLEMENTACAO.md, seguindo as regras da seção 2.`

---

## 1. Contexto e premissas

### 1.1 Estado de partida (verificado em 2026-11-23)

- `develop` está em `64c7d63` e contém: CI/CD em standby (`.github/workflows/{ci,build-images,deploy,security}.yml`), OpenSpec (`openspec/`), chatbot (mock), billing Abacate Pay (mock), RBAC `ASTRO_ADMIN | MANAGER | SELLER`, migrations **V1–V18**.
- A minha branch contém o módulo **`integrations/webmotors`** (polling da API de Consulta de Leads, caixa de entrada de propostas, aceite/recusa, credenciais cifradas, endpoints, 47 testes), mergeado com a `develop` de 2026-11-18 (`4825abc`). **Ainda não foi mergeado com a `develop` atual** (35 commits depois) — isso é a Fase 0.
- No merge com a `develop` atual, o único conflito textual é `backend/src/main/resources/application.yaml` (meu bloco no fim do arquivo). O único erro de compilação é `CrmLeadIntakeAdapter` (usa `LeadRepository.findByOrganizationIdAndPhoneNumber`, que a `develop` removeu). Ambos verificados num worktree descartável.
- A migration do módulo está como `V5__webmotors_leads_polling.sql` e **colide** com `V5__saas_plan_offers.sql` da `develop`. Deve virar a próxima versão livre (hoje **`V19`**; confirmar com `ls backend/src/main/resources/db/migration`).

### 1.2 Fonte de verdade para a Webmotors

**A collection Postman "API Lead" + a pesquisa (docs/integrations/webmotors.md, docs/INTEGRACOES-MARKETPLACES-META.md) valem mais do que o código Webmotors que já existia no `ingestion`.** O código antigo assume push por webhook, campos `NomeComprador/TelefoneComprador`, endpoint `/leads/v1/{id}` e autenticação em dois passos (token de app + `username=email`). Nada disso aparece na collection, que mostra: *password grant* (Basic `client_id:secret` + usuário/senha do integrador), polling em `GET /lead/v1/leads`, aceite/recusa em `PATCH /lead/v1/leads/{id}/aceite`, headers `client_id` e `access_token`.

### 1.3 Decisões assumidas (revisar; cada uma diz como reverter)

| # | Decisão assumida | Por quê | Como reverter |
|---|---|---|---|
| D1 | **Polling é o único caminho Webmotors.** Webhook/fetcher/auth Webmotors do `ingestion` ficam atrás de flag desligada (legado), removidos após a homologação. Mercado Livre continua no `ingestion`. | Confirmado pelo usuário: nossa pesquisa é mais atual que o código antigo. | Ligar `marketplace.webmotors.webhook.enabled=true` (Fase 1, T1.6). |
| D2 | **Aceite `AUTO` no piloto, `MANUAL` configurável por loja.** Hoje o padrão do código é `MANUAL`; passa a `AUTO` para novas contas. | Casa com o valor central ("lead acionável em segundos"). Confirmar na homologação o significado comercial de "aceitar". | Mudar o default em `AcceptMode`/migration e a nota da UI. |
| D3 | **Duplicidade segue a decisão vigente** (`isDuplicate=true` e ainda recebe mensagem), chave = só telefone. Lead sem telefone vira **pendente**, nunca é descartado. O adaptador atual (anexa nota ao lead existente) **será substituído**. | `openspec/STATUS.md` ("Decisões já tomadas"); evita contradizer o produto. | Reabrir regra #2 do STATUS; trocar a chave em `LeadIngestionService`. |
| D4 | **Mensagem de boas-vindas ligada por padrão** (como no spec `lead-ingestion`), desligável por loja (`welcome_message_enabled`). **Validar a base legal com o jurídico antes de ir a produção.** | Spec do produto exige o template `welcome_lead`. Meta ainda é mock. | Default `false` na migration. |
| D5 | **LGPD: registro de solicitações + anonimização + página pública + e-mail de privacidade.** Sem portal self-service do titular. A Astro é **operadora**; a concessionária é **controladora**. | Atende o formulário com trilha de auditoria sem custo de portal. | Estender depois com formulário público. |
| D6 | **Banco no deploy inicial:** Postgres no Compose com backup para S3 (staging); RDS só em produção. | Orçamento de teste US$ 100 (`infra/aws/README.md`). | Decisão do Ruy na change `aws-deploy-activation`. |
| D7 | **Hexagonal fica de fora** (decisão do usuário). Mantém-se arquitetura em camadas por módulo, com portas apenas onde já existem (`WebmotorsApi`, `CrmLeadIntake`, `MarketplaceLeadFetcher`). | Custo alto, ganho baixo diante das pendências. | — |
| D8 | **Mercado Livre:** sem módulo novo agora; só o hardening do `ingestion` (Fase 6). A integração real depende da pesquisa em andamento do usuário. | Comportamento diferente da Webmotors; evitar abstração prematura. | — |

---

## 2. Regras de trabalho (valem para todas as fases)

1. **Convenções do projeto** (`openspec/config.yaml`, `CLAUDE.md`): pacote canônico `com.astro.leadflow.shared.modules.*`; **uma classe por arquivo, sem DTO inline**; Flyway sequencial (`V*__*.sql`, nunca editar migration já mergeada); PT-BR; BRL; Lucide; sem dark mode por padrão; responsivo em 375/768/1024/1440.
2. **Multi-tenancy:** `organizationId` sempre via `SecurityUtils.getCurrentOrganizationId()` (JWT). Toda query de dado de loja filtra por organização. Chaves do React Query começam com `orgId`; logout limpa o cache.
3. **RBAC real do código:** `ASTRO_ADMIN` (interno, sem organização), `MANAGER`, `SELLER`. **Não existe role `ADMIN`.** `SecurityConfig` usa `hasRole("DEALERSHIP")` para as APIs de concessionária e `hasRole("MANAGER")` para DELETE e referral [VERIFICAR como `JwtAuthenticationFilter` mapeia MANAGER/SELLER → `DEALERSHIP` antes de assumir qualquer coisa].
4. **Segredos e PII:** nunca em log, nunca em teste/fixture reais, nunca em `localStorage`. Telefone/e-mail só mascarados em log (ver T1.5). Credenciais de marketplace só cifradas.
5. **Ambiente de build/test:**
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   export LC_ALL=C.UTF-8 LANG=C.UTF-8      # sem isso o Kotlin falha em testes com acento no nome
   cd backend && ./gradlew test --no-daemon
   cd ../frontend && npm ci && npm run lint && npm run test:coverage && npm run build
   ```
   `LeadflowApplicationTests` precisa de Postgres e Redis em `localhost` (ver `backend/compose.yaml` ou os `services:` de `ci.yml`). Sem eles, só essa classe falha — nunca "corrigir" mudando o teste.
6. **Testes primeiro quando houver regra de negócio.** Cobrir caminho feliz, erro, isolamento entre organizações e papel não autorizado.
7. **Commits pequenos** (`feat|fix|refactor|test|docs(escopo): ...`), um por tarefa. Sem `--no-verify`, sem force-push, sem reescrever histórico publicado.
8. **Não tocar** em `.github/workflows/` nem `infra/aws/`. Se a fase exigir mudança lá, descrever em "Pedido ao Ruy" no fim da fase.
9. **Dúvida de contrato Webmotors?** Não inventar. Marcar `// TODO(homologacao): ...`, registrar na tabela da Fase 9 e seguir com o comportamento defensivo já existente (campos anuláveis, `ignoreUnknown`).

---

## 3. Definition of Done (por tarefa e por fase)

- [ ] Compila (`./gradlew compileKotlin compileTestKotlin`) e a suíte do módulo tocado passa.
- [ ] Suíte completa: backend sem regressões (exceto `LeadflowApplicationTests` se não houver Postgres/Redis) e frontend `lint`, `test:coverage`, `build` verdes.
- [ ] Nenhuma classe duplicada de convenção (uma classe por arquivo) e nenhum import legado `com.astro.leadflow.modules.*`.
- [ ] Migrations aplicam em Postgres 16 limpo na ordem `V1…Vn` e batem com o que o Hibernate espera (rodar o app com `spring.jpa.hibernate.ddl-auto=validate` contra o banco migrado).
- [ ] Documentação atualizada (`docs/`, `openspec/STATUS.md`) e a change do OpenSpec com tarefas marcadas.
- [ ] Sem segredo/PII em código, logs ou fixtures.

---

## 4. Mapa de dependências

```
F0 Consolidar ──► F1 Alinhar Webmotors ──► F4 UI Webmotors ──► F9 Homologação*
        │                  │
        │                  └──► F6 Hardening ingestão (ML + sem telefone)
        ├──► F2 LGPD ──► F3 Dados institucionais/formulário ──► [ENVIAR FORMULÁRIO]
        ├──► F5 Credenciais em repouso
        └──► F7 Observabilidade ──► F8 Pré-deploy (código)
* F9 só depois que a Webmotors liberar o acesso.
```

**Caminho crítico para abrir a solicitação:** F0 → F2 → F3 (+ dados que só o Ruy tem: domínio, e-mail, CNPJ, logo). F1/F4 podem andar em paralelo e aumentam a credibilidade técnica na reunião de homologação.

Estimativas (ordem de grandeza, 1 desenvolvedor sênior ou agente): F0 0,5d · F1 2d · F2 3–4d · F3 1d (+ insumos) · F4 2–3d · F5 2d · F6 3d · F7 1d · F8 2d · F9 1–2d.

---

## FASE 0 — Consolidar a branch com a `develop` atual

**Change OpenSpec:** `webmotors-leads-polling` — *Why:* a Webmotors só oferece API de consulta (pull); o código Webmotors do `ingestion` assume push e não segue a collection. *What:* módulo `integrations/webmotors` (login password grant, polling, caixa de entrada, aceite/recusa, credenciais cifradas), desativar o fluxo legado, UI de gestão e homologação. *Impact:* `integrations/webmotors`, `ingestion`, `crm`, migrations, frontend (aba Integrações).

**Objetivo:** branch compilando e testada sobre a `develop` mais recente, pronta para receber mudanças.

**Passos**
1. `git fetch origin && git checkout claude/marketplace-integrations-ynztxc && git merge origin/develop`. (Merge, **não** rebase: o branch já foi publicado.)
2. Conflito em `application.yaml`: ficar com a versão da `develop` e reanexar no fim o bloco `integrations.webmotors` (comentário "Integração Webmotors" … `secrets-key`). O arquivo usa **CRLF**: manter CRLF no bloco. Conferir que não há chave `integrations:` duplicada.
3. Renomear a migration: `git mv .../V5__webmotors_leads_polling.sql .../V<próxima livre>__webmotors_leads_polling.sql` (hoje `V19`). Atualizar referências em `docs/integrations/webmotors.md` (seções 7 e 8).
4. Corrigir o único erro de compilação: `CrmLeadIntakeAdapter.kt` usa `findByOrganizationIdAndPhoneNumber`. **Não** recriar o método; a Tarefa T1.3 reescreve o adaptador. Para compilar já, trocar por `findFirstByOrganizationIdAndPhoneNumberOrderByCreatedAtAsc` (existe na `develop`) e manter o comportamento atual temporariamente.
5. Rodar a suíte completa (seção 2, regra 5). Esperado: tudo verde.
6. Validar a cadeia de migrations em Postgres limpo (`V1…V19`) e `ddl-auto=validate`.
7. `git push -u origin claude/marketplace-integrations-ynztxc`.

**Aceite:** `compileKotlin`/`compileTestKotlin` verdes; 47 testes `integrations.webmotors` verdes; nenhuma versão de migration duplicada.

- [ ] F0 concluída

---

## FASE 1 — Alinhar o módulo Webmotors ao produto e às convenções

**Objetivo:** o módulo passa a respeitar as regras do projeto (uma classe por arquivo, RBAC real, duplicidade/mensagem oficiais, sem PII em log) e o legado Webmotors é desativado.

### T1.1 — Uma classe por arquivo, sem DTO inline
Arquivos com mais de um tipo hoje (dividir; manter pacote e nomes):

| Arquivo | Dividir em |
|---|---|
| `infra/WebmotorsDtos.kt` (10 tipos) | `WmTokenResponse`, `WmLeadPage`, `WmLeadItem`, `WmLoja`, `WmAnuncio`, `WmVendedor`, `WmEnvelope`, `WmKeyValue`, `WmDecisionRequest`, `WmStatusRequest` (em `infra/dto/`) |
| `infra/WebmotorsApi.kt` | `WebmotorsApi`, `WmCredentials`, `WebmotorsApiException` |
| `api/WebmotorsController.kt` | controller + `WebmotorsAccountRequest`, `RefuseProposalRequest`, `ProposalResponse` (em `api/requests/` e `api/responses/`) + mapper `ProposalResponseMapper` |
| `service/WebmotorsAccountService.kt` | + `AccountCommand`, `AccountView`, `AccountRuntime` |
| `service/WebmotorsInboxService.kt` | + `InboxEntry` |
| `service/WebmotorsLeadSyncService.kt` | + `SyncReport` |
| `service/WebmotorsContactExtractor.kt` | + `WmContact` |
| `service/CrmLeadIntake.kt` | `CrmLeadIntake`, `IncomingLead`, `IntakeResult` |
| `domain/WebmotorsAccount.kt` | + `AcceptMode` |
| `domain/WebmotorsLeadInbox.kt` | + `InboxStatus` |
| `domain/WebmotorsRepositories.kt` | `WebmotorsAccountRepository`, `WebmotorsLeadInboxRepository` |

Refatoração pura: **zero mudança de comportamento**; os 47 testes devem passar sem alteração além de imports. Commit único `refactor(webmotors): uma classe por arquivo`.

### T1.2 — RBAC real
- `WebmotorsController`: trocar `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` por `hasRole('MANAGER')`.
- `SecurityConfig`: adicionar `.requestMatchers("/api/v1/integrations/**").hasRole("MANAGER")` **antes** do bloco `DEALERSHIP`. [VERIFICAR a ordem dos matchers e o mapeamento de roles.]
- `ASTRO_ADMIN` não tem organização: confirmar que `SecurityUtils.getCurrentOrganizationId()` devolve erro 403 limpo (não 500) e cobrir em teste.
- Testes: `SELLER` → 403; `MANAGER` de outra organização não enxerga proposta alheia (404); sem token → 401.

### T1.3 — Entrada no CRM pelo caminho oficial de ingestão
**Problema:** `CrmLeadIntakeAdapter` cria/anexa lead por conta própria e contradiz a decisão "duplicado = `isDuplicate=true` + ainda envia mensagem".

**Passos**
1. Em `ingestion/service/`, extrair de `LeadIngestionService` a lógica comum (duplicidade, criação, `isDuplicate`, `usageLimitService.incrementUsage`, `sendTemplate` com tratamento de `QuotaExceededException`/`ExternalServiceException`) para um método público reutilizável, por exemplo `LeadIngestionService.ingestResolved(organization, command): IngestionOutcome` (`command` com nome, telefone normalizado, e-mail, origem, interesse de veículo, notas, `sendWelcome`). `ingestFromWebhook` e `processFromQueue` passam a chamá-lo. **Não mudar o comportamento atual dessas duas entradas** (rodar `LeadIngestionServiceTest` existente [VERIFICAR nome] antes e depois).
2. [VERIFICAR] contagem de uso: o ramo de duplicado chama `incrementUsage` explicitamente e o ramo normal não; conferir se `MessageSenderService.sendTemplate` já incrementa e se há **dupla contagem** ou **ausência de contagem**. Documentar o que encontrar; corrigir só se for claramente bug e com teste.
3. Reescrever `CrmLeadIntakeAdapter` para delegar a esse método. Remover o `TransactionTemplate` e o anexo de notas ao lead existente.
4. `IncomingLead.sendWelcomeMessage` vem de `AccountRuntime.welcomeMessageEnabled`. Estágio inicial: `TALKING_TO_BOT` quando há mensagem; `WAITING_ATTENDANCE` quando `welcomeMessageEnabled=false`.
5. Testes: lead novo; telefone repetido → segundo lead com `isDuplicate=true` **e** mensagem enviada; cota excedida → lead criado sem mensagem; falha da Meta não desfaz o lead.

### T1.4 — Aceite AUTO por padrão e boas-vindas ligadas (D2, D4)
- `AcceptMode` default `AUTO` e `welcomeMessageEnabled` default `true` na entidade **e** na migration da Fase 0 (a tabela ainda não foi para produção, então editar a migration não customizada ainda é seguro **somente se ela não tiver sido mergeada na `develop`**; caso contrário, criar nova migration `ALTER`).
- Atualizar testes e `docs/integrations/webmotors.md` (seção 7.1).

### T1.5 — Mascarar PII em log
- Criar `shared/util/PiiMasker.kt` (`maskPhone("+5511987654321") -> "+55******4321"`, `maskEmail("a***@dominio.com")`) com testes.
- Aplicar nos logs que hoje imprimem telefone: `ingestion/service/LeadIngestionService.kt` (linhas ~44, 83, 96, 112, 121), `communication/service/BotService.kt` (~43), `communication/infra/SqsConsumer.kt` (~23), `communication/infra/MetaClient.kt`, `communication/meta/{Mock,Real}MetaApiClient.kt`, `iam/service/TwoFactorService.kt`. [VERIFICAR com `grep -rnE "log\.\w+\(.*(phone|Phone|email|toPhone)"` — a lista pode ter mudado.]
- Teste de arquitetura simples (JUnit) que falha se um arquivo de `modules/` tiver `log.*(...phone...)` sem `PiiMasker` — opcional, mas recomendado.

### T1.6 — Desativar o fluxo Webmotors legado (D1)
- `ingestion/api/controller/MarketplaceWebhookController.receiveWebmotors`: proteger com `@Value("\${marketplace.webmotors.webhook.enabled:false}")`; quando desligado responder **410 Gone** com corpo vazio e log `warn` sem payload.
- `WebmotorsLeadFetcher` e `WebmotorsAuthService`: `@ConditionalOnProperty(prefix="marketplace.webmotors.webhook", name=["enabled"], havingValue="true")`. Como `MarketplaceLeadConsumer` injeta `List<MarketplaceLeadFetcher>`, garantir que a lista vazia para `WEBMOTORS` não derruba o boot nem o consumer (teste de contexto).
- Comentar `@Deprecated("Substituído por integrations/webmotors; ver docs/integrations/webmotors.md")` e atualizar `openspec/specs/lead-ingestion/spec.md` (via change) para dizer que Webmotors entra por polling.
- **Não apagar** o código ainda (apagar após a homologação, Fase 9).

### T1.7 — Verificação final da fase
Suíte completa; `grep` mostrando que não há `hasAnyRole('ADMIN'`, nem imports legados, nem logs de telefone sem máscara.

- [ ] T1.1 · [ ] T1.2 · [ ] T1.3 · [ ] T1.4 · [ ] T1.5 · [ ] T1.6 · [ ] T1.7

---

## FASE 2 — LGPD: solicitações de titulares e anonimização (bloqueia o formulário)

**Change OpenSpec:** `lgpd-data-subject-requests` — *Why:* o formulário de acesso à Webmotors pergunta o canal oficial e o SLA para anonimização; hoje só existe `DELETE /leads/{id}` (remoção física). *What:* tabela de solicitações com prazo, serviço de anonimização que cobre todas as tabelas com PII de lead, endpoints para `MANAGER`, bloqueio de contato a leads anonimizados, expurgo de payloads brutos. *Impact:* `crm`, `communication`, `chatbot`, `ingestion`, `integrations/webmotors`, migrations, frontend (página de privacidade e aba).

> **Nota jurídica:** a Astro é **operadora**; a concessionária é **controladora** e responde ao titular. Os textos e prazos abaixo são tecnicamente consistentes com a LGPD (art. 18 e 19) mas **devem ser revisados por advogado** antes de publicar.

### Mapa de PII (verificado; reconfirmar lendo cada entidade)
| Onde | PII | Tratamento na anonimização |
|---|---|---|
| `leads` | `name`, `phone_number` (NOT NULL), `email`, `notes` (texto livre) | `name`→"Titular anonimizado"; `phone_number`→`ANON-<8 hex>` (≤ 20 chars, único); `email`→null; `notes`→null; `vehicle_interest`/`budget`/`stage` **mantidos** (métrica); novo `anonymized_at` |
| `chat_messages.content` | conteúdo livre | → `[mensagem removida]`; manter metadados |
| `bot_sessions.slot_values` | JSON com nome/veículo/orçamento | → `{}` |
| `marketplace_webhook_events.raw_payload` | payload bruto do marketplace | → `{}` para eventos com `lead_id` do titular; e **expurgo por retenção** (T2.5) |
| `webmotors_lead_inbox` | `message`, `raw_payload` (sem contato, mas texto livre) | → null para a proposta vinculada ao lead |
| `lead_stage_history`, `application_error`, `webhook_event_log` | [VERIFICAR] podem ter texto com PII em `errorMessage` | só se houver PII comprovada |
| Outros leads com **o mesmo telefone** (duplicados) | idem | anonimizar **todos** os leads da organização com o mesmo telefone/e-mail |

### T2.1 — Modelo de dados (migration nova, próxima versão livre)
- `ALTER TABLE leads ADD COLUMN anonymized_at TIMESTAMP;` + índice parcial `WHERE anonymized_at IS NULL` se útil.
- `CREATE TABLE data_subject_requests (id UUID PK, organization_id UUID NOT NULL FK, type VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL, subject_hash VARCHAR(64) NOT NULL, subject_hint VARCHAR(40), channel VARCHAR(30), received_at TIMESTAMP NOT NULL, due_at TIMESTAMP NOT NULL, completed_at TIMESTAMP, handled_by UUID, result_summary TEXT, created_at TIMESTAMP NOT NULL)`.
  - `type`: `ANONYMIZATION | ACCESS | CORRECTION | DELETION`; `status`: `RECEIVED | IN_PROGRESS | COMPLETED | REJECTED`.
  - `subject_hash` = HMAC-SHA256 do identificador normalizado (telefone E.164 ou e-mail em minúsculas) com segredo `privacy.hash-secret`; `subject_hint` = identificador mascarado. **Nunca** guardar o identificador em claro.
  - `due_at = received_at + 15 dias` (configurável `privacy.sla-days`, default 15).
- Entidade `DataSubjectRequest`, enums, repositório (uma classe por arquivo).

### T2.2 — Serviço de anonimização
`LeadAnonymizationService.anonymize(organizationId, identifier): AnonymizationReport` (`@Transactional`):
1. Normalizar o identificador (telefone via `NormalizationService.normalizePhone`; e-mail em minúsculas).
2. Localizar **todos** os leads da organização com aquele telefone/e-mail (inclui duplicados).
3. Aplicar a tabela do mapa de PII acima, tudo na mesma transação.
4. `AnonymizationReport(leadsAnonymized, messagesRedacted, sessionsCleared, eventsCleared, inboxCleared)` — **sem** PII.
5. Idempotente: segunda execução devolve zeros sem erro.
6. Blindagens: `MessageSenderService` e envio do bot **não** enviam para lead com `anonymized_at != null`; o inbound do WhatsApp não deve reassociar mensagem a lead anonimizado (placeholder nunca bate com telefone real).

Testes (obrigatórios): isolamento por organização (não toca lead de outra org); duplicados anonimizados juntos; idempotência; relatório sem PII; mensagens/sessões/payloads limpos; envio bloqueado após anonimizar; telefone/e-mail inexistente → relatório zerado e solicitação `COMPLETED` com resumo "nenhum dado encontrado".

### T2.3 — Endpoints (somente `MANAGER`; `SecurityConfig` com matcher `/api/v1/privacy/**`)
- `POST /api/v1/privacy/requests` `{ type, identifier, channel }` → cria solicitação (`RECEIVED`), calcula `due_at`, devolve id.
- `POST /api/v1/privacy/requests/{id}/execute` → para `ANONYMIZATION`: executa T2.2, grava `result_summary` (contagens), `completed_at`, `handled_by`.
- `POST /api/v1/privacy/requests/{id}/reject` `{ reason }`.
- `GET /api/v1/privacy/requests?status=` → lista com `overdue: boolean` (`due_at` vencido e não concluída).
- Substituir o `DELETE /leads/{id}` **como caminho de LGPD** por este fluxo (o `DELETE` continua existindo para remoção operacional; documentar a diferença).
- Contratos em `docs/API.md`/Insomnia (`docs/insomnia/`) e testes de controller (401/403/404/isolamento).

### T2.4 — Frontend (aba "Privacidade" em Configurações, `MANAGER` apenas)
- Seguir o padrão service/hook/componente (ver Fase 4). Lista de solicitações com prazo e selo "Atrasada"; formulário "Nova solicitação"; ação "Executar anonimização" com diálogo de confirmação que explica que é **irreversível**.
- Testes de componente e de hook (chaves `orgId`).

### T2.5 — Retenção de payloads brutos
- Job agendado (`@Scheduled`, `SchedulingConfig` já existe) que limpa `raw_payload` de `marketplace_webhook_events` mais antigos que `privacy.retention.webhook-payload-days` (default 90) e `raw_payload` de `webmotors_lead_inbox` de propostas `IMPORTED/REFUSED` com mais de N dias. Desligado por padrão em dev; ligado em `prod`.
- Teste com relógio fixo.

### T2.6 — Documentação operacional
`docs/RUNBOOK-PRIVACIDADE.md`: como receber um pedido (canal, validação de identidade pela controladora), como executar, SLA, o que **não** é apagado (métricas agregadas, notas fiscais/cobrança se houver obrigação legal), backups (anonimização não retroage a backups; descrever a janela de retenção).

- [ ] T2.1 · [ ] T2.2 · [ ] T2.3 · [ ] T2.4 · [ ] T2.5 · [ ] T2.6

---

## FASE 3 — Dados institucionais e pacote do formulário

**Change OpenSpec:** `marketplace-access-readiness` — *Why:* o formulário da Webmotors exige canal de suporte, site, logo, CNPJ, política de privacidade e canal LGPD que a plataforma ainda não expõe. *What:* páginas legais públicas, configuração institucional por ambiente, rascunho das respostas. *Impact:* frontend (landing/rotas públicas), `docs/`.

### T3.1 — Configuração institucional (frontend)
- `frontend/src/config/company.ts` lendo `VITE_COMPANY_NAME`, `VITE_COMPANY_CNPJ`, `VITE_SUPPORT_EMAIL`, `VITE_SUPPORT_PHONE`, `VITE_PRIVACY_EMAIL`, `VITE_SITE_URL`. Valores no `.env.example`. **Sem** valores reais no repositório.

### T3.2 — Páginas públicas
- Rotas `/privacidade` e `/termos` dentro de `LandingLayout` (`App.tsx`), `React.lazy`/`Suspense` como as demais.
- `LandingFooter.tsx`: trocar os `href: '#'` (linhas ~17 e ~100) pelas rotas.
- **Conteúdo da política** (rascunho em `FORMULARIO-WEBMOTORS.md`, seção B): papéis (operadora × controladora), dados tratados, finalidades, bases legais, compartilhamento (Meta/WhatsApp, Abacate Pay, AWS), retenção, direitos do titular e canal (`VITE_PRIVACY_EMAIL`), prazo de resposta, DPO/encarregado [PREENCHER]. Marcar claramente **"revisar com advogado"** no PR.
- Testes de rota e de snapshot de conteúdo mínimo (título, e-mail do canal).

### T3.3 — Ativos
- Logo em PNG (a Webmotors pede) em `frontend/public/brand/` (hoje só existem `favicon.ico`, `og-image.png`, `placeholder.svg`). **Arquivo vem do Ruy**; se não existir, exportar do componente `AstroLogo` e registrar a origem.

### T3.4 — Rascunho do formulário
Completar `docs/plano/FORMULARIO-WEBMOTORS.md` com os dados reais (campos `[PREENCHER]`) e fixar o texto final das três respostas dissertativas.

### Insumos que dependem do Ruy (bloqueiam o envio)
Razão social e CNPJ; **domínio** do site e e-mails (`suporte@`, `privacidade@`) com telefone; nome do solicitante e contato; **nome do app** a registrar no Portal do Desenvolvedor da Webmotors; logo PNG; regiões de atuação; CNPJ de ao menos uma concessionária em comum (piloto); ciência do SLA a declarar.

- [ ] T3.1 · [ ] T3.2 · [ ] T3.3 · [ ] T3.4

---

## FASE 4 — UI de gestão da Webmotors (aba "Integrações")

**Objetivo:** o gerente configura a conta, vê as propostas e aceita/recusa sem tocar na API.

**Padrão a seguir (ler antes):** `frontend/src/components/settings/ReferralTab.tsx`, `services/referrals.service.ts`, `hooks/api/useReferrals.ts`, `types/api/referrals.ts`, `test/ReferralTab.test.tsx`, `pages/Configuracoes.tsx` e `settings.helpers.ts` (`settingsTabs`).

### T4.1 — Contrato e camadas
- `frontend/src/types/api/webmotors.ts`: `WebmotorsAccount`, `WebmotorsAccountInput`, `WebmotorsProposal`, `WebmotorsProposalStatus`, `RefusalReason`, `SyncReport` — espelhando **exatamente** os DTOs do backend (`ProposalResponse`, `AccountView`, `SyncReport`, `WmKeyValue`).
- `services/webmotors.service.ts` (endpoints `/integrations/webmotors/...`) e `hooks/api/useWebmotors.ts` (React Query; chaves `[orgId, 'webmotors', ...]`; invalidar propostas após aceitar/recusar/sincronizar).

### T4.2 — Componentes (`components/settings/webmotors/`)
- `WebmotorsAccountForm` — `clientId`, `clientSecret`, usuário, senha, `storeId`, modo de aceite, boas-vindas, habilitado. **Nunca preencher segredos** com o valor salvo; mostrar `clientIdHint`; "em branco mantém o valor". Botão "Testar conexão".
- `WebmotorsSyncStatus` — último sync, sucesso/erro (`lastSyncError`), botão "Sincronizar agora" (trata 409/422 de sync em andamento).
- `WebmotorsProposalsTable` — filtro por status; cartões em mobile (como `Leads`/`Estoque`); ações Aceitar/Recusar; recusar abre diálogo com motivos de `GET /refusal-reasons`; estados de erro canônicos e esqueleto de carregamento; link para o lead quando `leadId` existe; selo para `AWAITING_CONTACT` com `attempts`.
- Aba `integracoes` registrada em `settingsTabs` (somente `MANAGER`; `SELLER` já vê apenas `general`/`security`).

### T4.3 — Testes
Serviço (mock HTTP), hook, componente (form sem exibir segredo; recusa exige motivo; ações desabilitadas fora de `PENDING`), e `ConfiguracoesPage.test.tsx` atualizado (aba some para `SELLER`). Responsividade 375/768/1024/1440 verificada no Playwright **ou** descrita no PR se E2E não rodar.

- [ ] T4.1 · [ ] T4.2 · [ ] T4.3

---

## FASE 5 — Credenciais em repouso (gap #13 da pesquisa)

**Change OpenSpec:** `credentials-encryption-at-rest`.

- Hoje: `marketplace_credentials` (`wm_client_secret`, `wm_access_token`, `ml_client_secret`, `ml_access_token`, `ml_refresh_token`) e `organizations.whatsapp_token` em **texto puro**. O módulo Webmotors já usa AES-256-GCM (`SecretCipher`).
- **T5.1** Mover `SecretCipher` para `shared/security/` (renomear `FieldCipher`; chave `app.security.field-encryption-key`, mantendo `integrations.webmotors.secrets-key` como alias de compatibilidade). Mesma API, mesmos testes.
- **T5.2** `EncryptedStringConverter` (`AttributeConverter<String?, String?>`) com prefixo `enc:v1:`. **Tolerante a texto puro** na leitura (valor sem prefixo = legado, devolve como está) e sempre cifra na escrita → migração gradual sem janela de indisponibilidade.
- **T5.3** Aplicar nos campos acima. Atenção ao tamanho (`TEXT` já comporta) e a queries que filtrem por esses campos (não deve haver; [VERIFICAR]).
- **T5.4** Comando/Job one-shot `reencrypt-legacy-credentials` (perfil `ops`, idempotente) que relê e regrava os registros legados.
- **T5.5** Fail-fast no perfil `prod`: aplicação não sobe com a chave ausente/ inválida (usar `@Validated` + `@ConfigurationProperties` ou `ApplicationRunner`) — **sem** imprimir a chave.
- **T5.6** Documentar geração e **rotação** da chave (`openssl rand -base64 32`; rotação = re-cifrar com a nova chave; manter `keyId` no prefixo para suportar duas chaves se precisar).
- Testes: round trip, leitura de legado, escrita cifrada (verificar no banco que o valor tem prefixo), prod sem chave não sobe.

- [ ] T5.1 · [ ] T5.2 · [ ] T5.3 · [ ] T5.4 · [ ] T5.5 · [ ] T5.6

---

## FASE 6 — Hardening da ingestão (Mercado Livre e lead sem telefone)

Executa a change existente `openspec/changes/ingestion-hardening` (tarefas 1.x–4.x), com os detalhes abaixo. **Não** criar módulo novo do Mercado Livre (D8).

- **T6.1 Entrada sem AWS** (tarefas 1.1–1.2): com `app.aws.enabled=false`, `MarketplaceWebhookController` processa inline (o `MarketplaceLeadConsumer` só existe com AWS). Extrair o corpo de `MarketplaceLeadConsumer.process` para um serviço reutilizável e chamá-lo do controller. Teste: webhook sem SQS cria o lead.
- **T6.2 Autenticidade do webhook** (2.1–2.3): remover o `return true` quando `marketplace.webhook-token` está vazio em `prod` (fail-fast no boot **e** 401 em runtime). Para o ML, validar assinatura HMAC **somente** depois de confirmar o formato com o ML; enquanto isso, validar consultando o recurso (`/vis/leads/{id}`) antes de aceitar. Resposta ao ML em ≤ 500 ms (responder 200 e processar assíncrono).
- **T6.3 Refresh serializado do token ML** (3.1–3.2): lock por credencial (por exemplo `SELECT … FOR UPDATE` na `marketplace_credentials` ou lock advisory do Postgres) em `MercadoLivreAuthService.refreshToken`; tratar `invalid_grant` marcando a credencial como `REAUTH_REQUIRED` e expondo isso na UI/alerta.
- **T6.4 Lead pendente sem telefone** (4.1) — **depende de D3**. Desenho: migration que torna `leads.phone_number` anulável + coluna `contact_status` (`COMPLETE|PENDING_CONTACT`); `NormalizationService` deixa de lançar para telefone ausente (continua lançando para telefone **inválido** presente?). [Decidir e documentar.] Ajustes: dedupe ignora nulo; `MessageSenderService` pula lead sem telefone; Kanban/lista mostram selo "Contato pendente"; ação `PATCH /leads/{id}/contact` para completar; testes de regressão do inbound WhatsApp. Para a Webmotors o equivalente já é `AWAITING_CONTACT` da caixa de entrada.
- **T6.5 Dados do anúncio do ML** (4.2): buscar marca/modelo/ano por `item_id` e preencher `vehicleInterest`.

- [ ] T6.1 · [ ] T6.2 · [ ] T6.3 · [ ] T6.4 · [ ] T6.5

---

## FASE 7 — Observabilidade e operação da integração

Seguir o padrão do módulo `monitoring` (`WebhookEventLog`, `ApplicationError`) e `infra/monitoring/prometheus/leadflow-alerts.yml`.

- **T7.1** Métricas Micrometer [VERIFICAR se Micrometer/Actuator já está nas dependências]: `integrations_webmotors_sync_total{outcome}`, `integrations_webmotors_proposals{status}` (gauge), `integrations_webmotors_api_errors_total{status}`, duração do sync.
- **T7.2** Regras de alerta em `leadflow-alerts.yml` (arquivo de monitoramento, não é CI/CD): sync falhando há > 15 min; propostas `PENDING` com mais de N minutos em conta `AUTO`; propostas em `FAILED`.
- **T7.3** `docs/RUNBOOK-WEBMOTORS.md`: sintomas → causa → ação (401 repetido, `AWAITING_CONTACT` crescente, lease preso, chave de cifra ausente), como forçar sync, como reprocessar `FAILED`.
- **T7.4** Painel do Astro Admin: contagem de contas Webmotors habilitadas e com erro (reaproveitar o módulo `admin` [VERIFICAR]).

- [ ] T7.1 · [ ] T7.2 · [ ] T7.3 · [ ] T7.4

---

## FASE 8 — Pré-deploy (lado do código)

Coordenar com a change `aws-deploy-activation` (CI/CD e AWS são do Ruy).

- **T8.1** Subir com `SPRING_PROFILES_ACTIVE=prod` localmente, `APP_AWS_ENABLED=false` e variáveis de teste; corrigir o que quebrar (risco conhecido em `infra/aws/README.md`: `SqsAutoConfiguration` não é excluído em `prod`).
- **T8.2** Variáveis novas em `infra/prod/.env.example` e no `docker-compose.prod.yml`: `WEBMOTORS_ENABLED=false`, `WEBMOTORS_SECRETS_KEY`, `WEBMOTORS_LEADS_AUTH_URL`, `WEBMOTORS_LEADS_API_URL`, `APP_SECURITY_FIELD_ENCRYPTION_KEY`, `PRIVACY_HASH_SECRET`, `PRIVACY_SLA_DAYS`, `PRIVACY_RETENTION_WEBHOOK_PAYLOAD_DAYS`, `VITE_*` institucionais. **Integração Webmotors fica desligada em produção até a homologação.**
- **T8.3** E-mail em produção (change `email-ses-prod`): `SesEmailService` (SMTP ou SDK) no perfil `prod`; o canal de privacidade e os convites dependem disso.
- **T8.4** Script de smoke test (`scripts/smoke-staging.sh`): login, criação de lead, listagem do Kanban, `GET /actuator/health`, `GET /api/v1/integrations/webmotors/account` (404 esperado sem conta), página `/privacidade`.
- **T8.5** Pedir ao Ruy (registrar em "Pedido ao Ruy" no fim da fase) o que for de `.github/`: rodar `ci.yml` com os novos testes e acrescentar um passo `npx @fission-ai/openspec validate --all` se desejar.

- [ ] T8.1 · [ ] T8.2 · [ ] T8.3 · [ ] T8.4 · [ ] T8.5

---

## FASE 9 — Homologação Webmotors (somente após o acesso)

Cada linha é uma **suposição** do módulo. Para cada uma: obter amostra real **sanitizada**, guardar em `backend/src/test/resources/webmotors/`, escrever teste de contrato e ajustar o ponto indicado.

| # | Suposição | Onde ajustar | Como confirmar |
|---|---|---|---|
| H1 | Login `grant_type=password` com Basic `client_id:secret`, resposta HTTP 200/201 com `access_token`/`expires_in` | `WebmotorsHttpClient.token` | Chamada real em homologação |
| H2 | Headers dos dados: `client_id` + `access_token` (não `Authorization: Bearer`) | `WebmotorsHttpClient.authed` | idem |
| H3 | Nome do parâmetro de paginação (`pagina`) e semântica de `paginaAtual/totalPaginas` | `WebmotorsProperties.pageParam`, `WebmotorsLeadSyncService` | Listagem com > 1 página |
| H4 | Formato do contato no detalhe (`nome/email/telefone`, nível de aninhamento) e se o telefone vem mascarado antes do aceite | `WebmotorsContactExtractor` | `GET /lead/v1/leads/{id}` antes e depois do aceite |
| H5 | Aceitar não envia `codigo_motivo` | `WebmotorsHttpClient.accept` | `PATCH .../aceite` real |
| H6 | Significado de `tipoLead` (1, 2, `subTipoLead`) e `id_canal` | `GET /tipos`, `GET /canais` (expostos crus) | Resposta real; mapear para `typeDescription` |
| H7 | Formato do envelope de motivos (`Sucesso/Retorno/Mensagens`) | `WmEnvelope`, `refusalReasons` | `GET /motivos-recusa` |
| H8 | IDs numéricos tratados como texto; datas como texto | `WmLeadItem` | Amostra real |
| H9 | Rate limit e intervalo mínimo de polling | `WebmotorsProperties.pollIntervalMs` | Combinar com a Webmotors |
| H10 | `https` nos endpoints de dados (a collection de homologação usa `http`) e URLs de produção | `application.yaml` | Documentação de produção |
| H11 | `loja.id` casa com `store_id` configurado | `WebmotorsLeadSyncService` | Amostra real |
| H12 | O significado comercial de "aceitar" (D2) e se aceitar sem atender penaliza a loja | decisão de produto | Reunião com a Webmotors |

**Depois da homologação:** remover o legado Webmotors do `ingestion` (controller, fetcher, auth service, colunas `wm_*` de `marketplace_credentials` via migration), atualizar `openspec/specs/lead-ingestion/spec.md`, ligar `WEBMOTORS_ENABLED=true` em staging e depois produção, e arquivar a change (`/openspec-archive-change`).

- [ ] F9 concluída

---

## 5. Trilhas paralelas já existentes no OpenSpec (não duplicar)

| Change | Estado | Bloqueio | Observação para este plano |
|---|---|---|---|
| `aws-deploy-activation` | standby | Conta AWS (Ruy) | F8 coordena o lado do código |
| `email-ses-prod` | pendente | Domínio + conta AWS | T8.3; **necessário** para o canal de privacidade/convites |
| `meta-whatsapp-real` | pendente | Verificação de negócio, número, token, template `welcome_lead` | D4 (boas-vindas) depende disso para funcionar de verdade; custo/classificação do template ainda em aberto |
| `lex-real-engine` | pendente | Conta AWS | Independente das fases acima |
| `abacatepay-sandbox-validation` | pendente | Conta/chave sandbox | Regras #6–#8 do STATUS seguem abertas |
| `ingestion-hardening` | pendente | Webmotors/ML | Executada na F6 |

---

## 6. Ações do Ruy (fora do código)

1. **Webmotors:** registrar o app no Portal do Desenvolvedor (obter Client ID/Secret), preencher o formulário (rascunho em `FORMULARIO-WEBMOTORS.md`), pedir ambiente de homologação e confirmar H3–H12.
2. **Dados institucionais** da Fase 3 (domínio, e-mails, CNPJ, logo, regiões, piloto).
3. **Mercado Livre:** app em developers, redirect URI, tópico `vis_leads`, conta vendedora Motors (pesquisa em andamento).
4. **Meta:** Business Portfolio/verificação, número dedicado, token permanente, template `welcome_lead`.
5. **AWS/CI/CD:** conta, Budget, OIDC, EC2/SSM, SES, domínio/DNS.
6. **Jurídico:** revisar política de privacidade, base legal do contato inicial por WhatsApp, papéis operadora × controladora e SLA declarado.

---

## 7. Registro de riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Contrato real da Webmotors diferir da collection | Retrabalho em client/extractor | Tudo anulável + `ignoreUnknown`; F9 com testes de contrato; fluxo legado só removido depois |
| Dois fluxos Webmotors ativos criarem lead duplicado | Mensagem dupla ao cliente | D1: legado atrás de flag desligada |
| Anonimização esquecer uma tabela com PII | Descumprimento LGPD | Mapa de PII revisado na T2.2 + teste que varre colunas por nome (`phone`, `email`, `name`) e falha se houver coluna nova sem decisão |
| Perda da chave de cifra | Credenciais irrecuperáveis | Backup da chave em cofre (SSM Parameter Store), `keyId` no prefixo, documento de rotação |
| `phone_number` anulável (T6.4) quebrar dedupe/Kanban/inbound | Regressão em fluxo central | Migração incremental, testes de regressão do inbound WhatsApp, flag por etapa |
| Migrations fora de ordem entre branches | Falha de deploy | Sempre conferir a próxima versão livre no momento do merge (F0 passo 3) |
| Meta/AWS ainda em mock | "Funciona" sem prova real | Registrar como não-validado em `openspec/STATUS.md` até haver teste real |

---

## 8. Checklist final de aceitação do plano

- [ ] Branch consolidada com a `develop`, suíte verde, migrations em ordem (F0)
- [ ] Webmotors alinhado ao produto/convenções e legado desativado (F1)
- [ ] LGPD: solicitações + anonimização + retenção + runbook (F2)
- [ ] Páginas legais e rascunho do formulário prontos; insumos do Ruy recebidos (F3)
- [ ] UI Webmotors operando contra o backend (F4)
- [ ] Credenciais cifradas em repouso e fail-fast em `prod` (F5)
- [ ] Ingestão ML endurecida e política de lead pendente aplicada (F6)
- [ ] Métricas, alertas e runbook da integração (F7)
- [ ] `prod` sobe localmente; `.env.example` e smoke test prontos (F8)
- [ ] **Formulário enviado à Webmotors**
- [ ] Homologação concluída e legado removido (F9)
