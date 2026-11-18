-- Integração Webmotors: conta por loja (credenciais cifradas) e caixa de entrada de propostas.
-- NOTA: o arquivo de init existente se chama "v1__..." (v minúsculo); o Flyway só reconhece "V".
-- Se o Flyway for habilitado, renomear v1__init_schema.sql para V1__init_schema.sql.

CREATE TABLE webmotors_accounts (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id          UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    store_id                 VARCHAR(255),
    client_id_enc            VARCHAR(1000) NOT NULL,
    client_secret_enc        VARCHAR(1000) NOT NULL,
    username_enc             VARCHAR(1000) NOT NULL,
    password_enc             VARCHAR(1000) NOT NULL,
    accept_mode              VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    welcome_message_enabled  BOOLEAN       NOT NULL DEFAULT FALSE,
    enabled                  BOOLEAN       NOT NULL DEFAULT TRUE,
    last_sync_at             TIMESTAMP,
    last_sync_ok             BOOLEAN,
    last_sync_error          VARCHAR(500),
    lease_until              TIMESTAMP,
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP
);

CREATE TABLE webmotors_lead_inbox (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id    UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    account_id         UUID NOT NULL REFERENCES webmotors_accounts(id) ON DELETE CASCADE,
    external_id        VARCHAR(100) NOT NULL,
    lead_type          INT          NOT NULL DEFAULT 0,
    status             VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    message            TEXT,
    channel            VARCHAR(100),
    type_description   VARCHAR(255),
    remote_status      VARCHAR(100),
    remote_created_at  VARCHAR(100),
    ad_id              VARCHAR(100),
    ad_brand           VARCHAR(100),
    ad_model           VARCHAR(150),
    ad_version         VARCHAR(255),
    ad_year            INT,
    ad_plate           VARCHAR(20),
    ad_price           NUMERIC(12,2),
    raw_payload        TEXT,
    refusal_code       INT,
    refusal_message    VARCHAR(500),
    lead_id            UUID REFERENCES leads(id) ON DELETE SET NULL,
    attempts           INT          NOT NULL DEFAULT 0,
    last_error         VARCHAR(500),
    received_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    decided_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    UNIQUE (organization_id, external_id, lead_type)
);

CREATE INDEX idx_wm_inbox_org_status ON webmotors_lead_inbox(organization_id, status);
