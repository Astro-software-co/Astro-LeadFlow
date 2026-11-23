## Why
A Webmotors só oferece a API de Consulta de Leads (pull, *password grant*, `GET /lead/v1/leads`, aceite/recusa em `PATCH /lead/v1/leads/{id}/aceite`), como mostra a collection Postman "API Lead". O código Webmotors do módulo `ingestion` assume push por webhook, outro contrato e outra autenticação, e nunca rodou contra a Webmotors real. Além disso, logs de ERROR são persistidos em `application_error`, então telefone, e-mail ou token em log viram dado pessoal gravado no banco.

## What Changes
- Módulo `integrations/webmotors` (polling agendado com lease, caixa de entrada de propostas, aceite/recusa, credenciais por loja cifradas) passa a ser o **único** caminho Webmotors.
- **BREAKING (interno):** remoção do fluxo legado Webmotors do `ingestion` (endpoint `POST /api/v1/webhooks/webmotors`, `WebmotorsLeadFetcher`, `WebmotorsAuthService`, colunas `wm_*` de `marketplace_credentials`, propriedades `marketplace.webmotors.*`). Mercado Livre não muda.
- Endpoints `/api/v1/integrations/webmotors/**` restritos a `MANAGER` (antes `ADMIN|MANAGER`, role inexistente).
- Lead aceito entra no CRM pela mesma regra de criação da ingestão oficial (normalização, cota, duplicidade `isDuplicate=true` + mensagem), por uma única porta.
- Padrões por loja: aceite `AUTO`; mensagem de abordagem ligada.
- Máscara global de PII e segredos nos logs (console e `application_error`), sem corpo/payload de terceiros em log e sem valor cru em mensagens de exceção.

## Capabilities

### New Capabilities
- `webmotors-integration`: conta Webmotors por organização, sincronização por polling, caixa de entrada de propostas, aceite/recusa e entrada do lead aceito no CRM.
- `log-redaction`: garantia de que logs (console e erros persistidos) não carregam telefone, e-mail, tokens nem segredos.

### Modified Capabilities
- `lead-ingestion`: a Webmotors deixa de entrar por webhook e passa a entrar por polling.

## Impact
- Backend: `integrations/webmotors` (divisão em uma classe por arquivo, RBAC, porta `CrmLeadIntake`), `ingestion` (`LeadIngestionService.ingestResolved`, remoção do legado), `saas/domain/MarketplaceCredential`, `config/SecurityConfig`, `shared/util/PiiMasker`, `logback-spring.xml`, `monitoring/error/ApplicationErrorWriter`, clientes Meta, `TwoFactorService`, `BotService`, `SqsConsumer`, `AbacatePayClient`.
- Migrations: `V19__webmotors_leads_polling.sql` (defaults), nova `V20` (drop `wm_*`).
- Configuração: `application.yaml`, `application-prod.yml`, `infra/prod/.env.example`.
- Docs: `docs/integrations/webmotors.md`, `docs/INTEGRACOES-MARKETPLACES-META.md`, `openspec/STATUS.md`.
- Frontend: nenhum nesta change (UI na phase G7).
