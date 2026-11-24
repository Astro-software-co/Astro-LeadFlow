## Why
O formulário de acesso à Webmotors pergunta o canal oficial e o SLA para anonimização de dados de titulares. Hoje só existe `DELETE /leads/{id}`, que falha para leads com evento de marketplace ou sessão de bot (FKs sem `ON DELETE`), e não há registro de solicitações, anonimização nem retenção de payloads brutos. A Astro é operadora; a concessionária (controladora) precisa de ferramenta para atender o titular (LGPD art. 18 e 19).

## What Changes
- Correção do `DELETE /leads/{id}`: FKs de `marketplace_webhook_events` (`SET NULL`) e `bot_session` (`CASCADE`).
- Registro de solicitações de titulares com prazo (`data_subject_requests`), guardando só HMAC e dica mascarada do identificador.
- Anonimização por telefone/e-mail cobrindo `leads`, `chat_messages`, `bot_session`, `marketplace_webhook_events`, `webmotors_lead_inbox` e `application_error`.
- Bloqueio de envio de mensagens do sistema para lead anonimizado.
- Endpoints `/api/v1/privacy/**` (somente `MANAGER`) e aba "Privacidade" em Configurações.
- **Exclusão manual de lead** pelo dono da loja, com log de auditoria sem PII e opção de excluir os demais atendimentos do mesmo telefone.
- Job de retenção: limpa `raw_payload` de eventos de marketplace e da caixa Webmotors após 90 dias e apaga `application_error` após 90 dias (retenção que existia só como propriedade).
- Runbook de privacidade.
- Gaps herdados da G1 corrigidos nesta change: 401 global sem token (G1-01), rollback-only por falta de credencial WhatsApp (G1-03), erros da aplicação no `application_error` fora do `prod` (G1-04).

## Capabilities

### New Capabilities
- `privacy-data-subject-requests`: solicitações de titulares, anonimização, bloqueio de contato a anonimizados e retenção de dados brutos.
- `lead-deletion`: exclusão manual e irreversível de lead e dados dependentes, com auditoria sem PII.

### Modified Capabilities
_(nenhuma spec existente muda de requisito)_

## Impact
- Backend: novo módulo `privacy` (domínio, serviços, controller, job), `crm` (exclusão), `communication` (bloqueio de envio), `config/SecurityConfig`, `logback-spring.xml`.
- Migrations: `V21` (FKs + `anonymized_at` + `data_subject_requests` + `lead_deletion_log`).
- Frontend: aba Privacidade; ações "Excluir lead" e "Anonimizar" no detalhe do lead e no card do Kanban.
- Docs: `docs/RUNBOOK-PRIVACIDADE.md`, `docs/API.md`, Insomnia. **Textos e prazos: revisar com advogado.**
