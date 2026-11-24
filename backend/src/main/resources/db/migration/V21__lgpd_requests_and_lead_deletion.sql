-- LGPD (phase G2): exclusão de lead sem violar FK, anonimização, solicitações de titulares e auditoria de exclusão.

-- T2.0: lead apagado não pode travar em eventos de marketplace nem em sessões do bot.
ALTER TABLE marketplace_webhook_events DROP CONSTRAINT IF EXISTS marketplace_webhook_events_lead_id_fkey;
ALTER TABLE marketplace_webhook_events
    ADD CONSTRAINT marketplace_webhook_events_lead_id_fkey FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE SET NULL;

ALTER TABLE bot_session DROP CONSTRAINT IF EXISTS bot_session_lead_id_fkey;
ALTER TABLE bot_session
    ADD CONSTRAINT bot_session_lead_id_fkey FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;

ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_bot_session_id_fkey;
ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_bot_session_id_fkey FOREIGN KEY (bot_session_id) REFERENCES bot_session(id) ON DELETE SET NULL;

-- T2.1: marca de anonimização (dados pessoais removidos, card e métricas mantidos).
ALTER TABLE leads ADD COLUMN anonymized_at TIMESTAMP;

-- T2.1: solicitações de titulares. Nunca guarda o identificador em claro (só HMAC e dica mascarada).
CREATE TABLE data_subject_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    subject_hash    VARCHAR(64) NOT NULL,
    subject_hint    VARCHAR(40),
    channel         VARCHAR(30),
    received_at     TIMESTAMP NOT NULL,
    due_at          TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP,
    handled_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    result_summary  TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dsr_type CHECK (type IN ('ANONYMIZATION', 'ACCESS', 'CORRECTION', 'DELETION')),
    CONSTRAINT chk_dsr_status CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'))
);
CREATE INDEX idx_dsr_org_status ON data_subject_requests(organization_id, status);

-- T2.7: auditoria de exclusão manual, sem dado pessoal.
CREATE TABLE lead_deletion_log (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    deleted_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    deleted_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    subject_hash     VARCHAR(64) NOT NULL,
    cards_deleted    INT NOT NULL,
    messages_deleted INT NOT NULL,
    reason           VARCHAR(500)
);
CREATE INDEX idx_lead_deletion_log_org ON lead_deletion_log(organization_id, deleted_at DESC);
