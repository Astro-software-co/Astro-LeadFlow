CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    whatsapp_token              TEXT,
    whatsapp_phone_number_id    VARCHAR(100),
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT         NOT NULL,
    phone_number    VARCHAR(20),
    avatar_url      TEXT,
    role            VARCHAR(30)  NOT NULL DEFAULT 'SELLER',
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    two_factor_email_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_sms_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_code             VARCHAR(10),
    two_factor_code_expires_at  TIMESTAMP,
    notify_new_lead         BOOLEAN NOT NULL DEFAULT TRUE,
    notify_lead_waiting     BOOLEAN NOT NULL DEFAULT TRUE,
    notify_sale_completed   BOOLEAN NOT NULL DEFAULT TRUE,
    notify_card_moved       BOOLEAN NOT NULL DEFAULT FALSE,
    notify_via_email        BOOLEAN NOT NULL DEFAULT FALSE,
    notify_via_push         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE vehicles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    brand           VARCHAR(100) NOT NULL,
    model           VARCHAR(100) NOT NULL,
    year            INT          NOT NULL,
    color           VARCHAR(50),
    license_plate   VARCHAR(20),
    sale_price      NUMERIC(12,2) NOT NULL,
    cost_price      NUMERIC(12,2),
    mileage_km      INT,
    ipva_paid       BOOLEAN NOT NULL DEFAULT FALSE,
    category        VARCHAR(30) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_organization_id ON vehicles(organization_id);
CREATE INDEX idx_vehicles_status ON vehicles(status);

CREATE TABLE vehicle_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id  UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    s3_url      TEXT NOT NULL,
    position    INT  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE leads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    assigned_to     UUID REFERENCES users(id) ON DELETE SET NULL,
    vehicle_id      UUID REFERENCES vehicles(id) ON DELETE SET NULL,
    name            VARCHAR(255),
    phone_number    VARCHAR(20) NOT NULL,
    email           VARCHAR(255),
    origin          VARCHAR(50) NOT NULL,
    vehicle_interest    TEXT,
    budget              NUMERIC(12,2),
    urgency             VARCHAR(30),
    purchase_forecast   VARCHAR(100),
    interest_type       VARCHAR(30),
    notes               TEXT,
    pipeline_stage  VARCHAR(50) NOT NULL DEFAULT 'TALKING_TO_BOT',
    is_duplicate    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leads_organization_id ON leads(organization_id);
CREATE INDEX idx_leads_pipeline_stage ON leads(pipeline_stage);
CREATE INDEX idx_leads_phone_number ON leads(phone_number);

CREATE TABLE lead_stage_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id     UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    moved_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    from_stage  VARCHAR(50),
    to_stage    VARCHAR(50) NOT NULL,
    moved_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id         UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    direction       VARCHAR(10)  NOT NULL,
    content         TEXT         NOT NULL,
    message_type    VARCHAR(30)  NOT NULL DEFAULT 'TEXT',
    wa_message_id   VARCHAR(100),
    status          VARCHAR(30)  NOT NULL DEFAULT 'SENT',
    sent_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_lead_id ON chat_messages(lead_id);

CREATE TABLE plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL UNIQUE,
    monthly_price_brl   NUMERIC(10,2) NOT NULL,
    message_limit       INT NOT NULL,
    marketplace_limit   INT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    plan_id         UUID NOT NULL REFERENCES plans(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    renews_at       DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE marketplace_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace_name    VARCHAR(50) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_limit       INT NOT NULL DEFAULT 200,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, marketplace_name)
);

CREATE TABLE usage_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace_name    VARCHAR(50),
    messages_sent       INT NOT NULL DEFAULT 0,
    period_month        INT NOT NULL,
    period_year         INT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, marketplace_name, period_month, period_year)
);

INSERT INTO plans (id, name, monthly_price_brl, message_limit, marketplace_limit) VALUES
  (gen_random_uuid(), 'Básico',       149.00, 1000, 1),
  (gen_random_uuid(), 'Profissional', 299.00, 3000, 3),
  (gen_random_uuid(), 'Enterprise',   599.00, 10000, 10);