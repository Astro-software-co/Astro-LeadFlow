-- Adiciona CNPJ à tabela organizations (necessário para resolver org por CNPJ no webhook Webmotors)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS cnpj VARCHAR(20);

-- Credenciais por marketplace por organização
CREATE TABLE marketplace_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    marketplace_type    VARCHAR(30) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    last_reconciled_at  TIMESTAMP,

    -- Webmotors
    wm_client_id        TEXT,
    wm_client_secret    TEXT,
    wm_access_token     TEXT,
    wm_token_expires_at TIMESTAMP,
    wm_integrator_email TEXT,
    wm_cnpj             VARCHAR(20),

    -- Mercado Livre
    ml_app_id           TEXT,
    ml_client_secret    TEXT,
    ml_access_token     TEXT,
    ml_refresh_token    TEXT,
    ml_token_expires_at TIMESTAMP,
    ml_user_id          BIGINT,

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_marketplace_per_org UNIQUE (organization_id, marketplace_type),
    CONSTRAINT chk_marketplace_type CHECK (
        marketplace_type IN ('WEBMOTORS', 'MERCADO_LIVRE', 'OLX')
    )
);

-- Log de cada notificação webhook recebida
CREATE TABLE marketplace_webhook_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marketplace_type    VARCHAR(30) NOT NULL,
    organization_id     UUID REFERENCES organizations(id),
    external_lead_id    TEXT,
    raw_payload         JSONB NOT NULL,
    received_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    status              VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    -- RECEIVED → QUEUED → PROCESSING → INGESTED | FAILED | QUOTA_EXCEEDED | DUPLICATE | IGNORED
    sqs_message_id      TEXT,
    processed_at        TIMESTAMP,
    error_message       TEXT,
    lead_id             UUID REFERENCES leads(id)
);

-- Log de reconciliação (job diário)
CREATE TABLE marketplace_reconciliation_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credential_id       UUID NOT NULL REFERENCES marketplace_credentials(id),
    started_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at         TIMESTAMP,
    leads_found         INT DEFAULT 0,
    leads_new           INT DEFAULT 0,
    leads_already_seen  INT DEFAULT 0,
    status              VARCHAR(20),
    error_message       TEXT
);

-- Índices
CREATE INDEX idx_webhook_events_org ON marketplace_webhook_events(organization_id);
CREATE INDEX idx_webhook_events_status ON marketplace_webhook_events(status);
CREATE INDEX idx_webhook_events_ext_lead ON marketplace_webhook_events(external_lead_id, marketplace_type);
CREATE INDEX idx_credentials_org ON marketplace_credentials(organization_id);
CREATE INDEX idx_credentials_active ON marketplace_credentials(marketplace_type) WHERE is_active = true;
