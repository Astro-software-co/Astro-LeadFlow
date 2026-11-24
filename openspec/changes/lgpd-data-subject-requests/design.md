## Context
Ver proposal.md. Verificado no código (2026-11-23):
- `marketplace_webhook_events.lead_id` (V3) e `bot_session.lead_id` (V13) referenciam `leads` sem `ON DELETE` → `DELETE /leads/{id}` falha com evento de marketplace ou sessão de bot. `chat_messages` e `lead_stage_history` já têm `CASCADE`; `webmotors_lead_inbox.lead_id` tem `SET NULL`. `chat_messages.bot_session_id` referencia `bot_session` sem `ON DELETE`.
- `lead_stage_history` não tem `error_message`; `webhook_event_log` não referencia lead (só código HTTP/erro técnico) — sem PII comprovada, fora do mapa.
- `MonitoringConfig.errorRetentionDays=90` existe, mas nenhum job apaga `application_error`.
- Tabelas da Fase 4 (`lead_interests`, `lead_interest_events`, `notifications`, `contact_consents`) ainda não existem.

## Goals / Non-Goals
**Goals:** solicitações com prazo, anonimização completa e idempotente, exclusão manual auditada, retenção, gaps G1-01/03/04.
**Non-Goals:** portal do titular (B4); tabelas da Fase 4 (cada phase que as cria estende anonimização e exclusão); fail-fast de `privacy.hash-secret` em `prod` (G8/T6.6).

## Decisions
1. **Módulo `privacy`** (`shared/modules/privacy/{domain,service,api,job}`), camadas como os demais (D7).
2. **Hash do titular:** HMAC-SHA256 com `privacy.hash-secret` sobre o identificador normalizado (telefone E.164 via `NormalizationService`; e-mail minúsculo). `SubjectHasher` é usado também pelo log de exclusão. Dev tem um segredo padrão só de desenvolvimento; `prod` exige a variável (fail-fast fica na G8).
3. **Execução reenvia o identificador:** como só o hash é guardado, `POST /requests/{id}/execute` recebe o identificador e confere o HMAC (422 se divergir). `ANONYMIZATION` anonimiza; `DELETION` exclui todos os leads do identificador (via `LeadDeletionService`, com log); `ACCESS`/`CORRECTION` são atendidos manualmente pela controladora e concluídos com `resultSummary` informado.
4. **Anonimização por SQL em lote** (`@Modifying` JPQL/nativo) dentro de uma transação: `phone_number='ANON-'||8 hex`, `name='Titular anonimizado'`, `email/notes=NULL`, `anonymized_at=now`; mensagens → `[mensagem removida]`; `slot_values='{}'`; eventos/inbox com `raw_payload` limpo. `application_error`: `replace()` das variantes do identificador (E.164, só dígitos, nacional, e-mail) pela máscara, nas linhas da organização ou sem organização (erros de plataforma).
5. **Bloqueio de envio:** `MessageSenderService` recusa lead com `anonymizedAt != null` com `RecipientBlockedException` (incluída em `noRollbackFor`). Inbound: o telefone anonimizado não casa com nenhum número real.
6. **Exclusão:** `LeadDeletionService` apaga explicitamente na ordem `chat_messages` → `bot_session` → `webmotors_lead_inbox` → lead (histórico em cascata); eventos de marketplace: `raw_payload` limpo e FK `SET NULL`. Log em `lead_deletion_log`. `DELETE /api/v1/leads/{id}?includeSamePhone=&reason=`.
7. **Retenção:** `PrivacyRetentionJob` (`@Scheduled` cron diário, `privacy.retention.enabled`, `false` em dev, `true` em `prod`), com `Clock` injetável.
8. **Gaps G1:** entry point 401 global (`HttpStatusEntryPoint`); `IllegalArgumentException` de `resolveWhatsappConfig` vira `ExternalServiceException` (já em `noRollbackFor`); logback fora do `prod`: `com.astro.leadflow` passa a enviar também para `ASYNC_DB_ERROR`.

## Risks / Trade-offs
- [Anonimização não alcança backups] → runbook descreve a janela de retenção dos backups.
- [`replace()` em `application_error` só cobre formatos conhecidos] → logs novos já saem mascarados (G1); a retenção de 90 dias remove o resto.
- [401 global muda o contrato de endpoints sem token] → o frontend já trata 401 e 403 da mesma forma (refresh/logout).
