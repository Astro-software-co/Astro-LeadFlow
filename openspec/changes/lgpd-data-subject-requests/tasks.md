# Tasks

## 0. Gaps herdados da G1
- [x] 0.1 G1-01: 401 sem token em toda a API (teste de segurança)
- [x] 0.2 G1-03: falta de credencial WhatsApp não marca rollback-only (teste de transação)
- [x] 0.3 G1-04: erros da aplicação chegam ao `application_error` fora do `prod` (teste de configuração do logback)

## 1. Banco (T2.0, T2.1)
- [x] 1.1 V21: FKs (`marketplace_webhook_events` SET NULL, `bot_session` e `chat_messages.bot_session_id` CASCADE), `leads.anonymized_at`, `data_subject_requests`, `lead_deletion_log`; entidades, enums e repositórios; teste de DELETE de lead com evento (falha antes, passa depois)

## 2. Anonimização (T2.2)
- [x] 2.1 `SubjectHasher` e `LeadAnonymizationService` com relatório sem PII; testes: isolamento, vários leads, idempotência, sem dados, relatório sem PII
- [x] 2.2 Bloqueio de envio a lead anonimizado e inbound sem reassociação; testes

## 3. API (T2.3)
- [x] 3.1 `/api/v1/privacy/requests` (criar, listar com `overdue`, executar, rejeitar); testes 401/403/404/422/isolamento; `docs/API.md` e Insomnia

## 4. Exclusão manual (T2.7 backend)
- [x] 4.1 `LeadDeletionService` + `lead_deletion_log` + `DELETE /leads/{id}?includeSamePhone`; testes por tabela, isolamento, SELLER 403, mesmo telefone, auditoria sem PII

## 5. Retenção (T2.5)
- [x] 5.1 `PrivacyRetentionJob` (payloads brutos + `application_error`) com relógio fixo; desligado em dev, ligado em `prod`

## 6. Frontend (T2.4, T2.7 frontend)
- [x] 6.1 Aba Privacidade (service/hook/componente, chaves `orgId`, testes)
- [x] 6.2 "Excluir lead" e "Anonimizar" no detalhe e no card do Kanban (MANAGER), diálogo irreversível, invalidação de cache; testes

## 7. Documentação e verificação (T2.6)
- [x] 7.1 `docs/RUNBOOK-PRIVACIDADE.md`; `openspec/STATUS.md`
- [x] 7.2 Suíte completa backend/frontend; migrations em Postgres limpo com `validate`

## Workflow follow-up
- As phases G4–G6 estendem anonimização e exclusão às tabelas novas (`lead_interests`, `lead_interest_events`, `notifications`, `contact_consents` — esta última preservada, R20).
