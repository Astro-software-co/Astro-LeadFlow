-- ── Phase 13: Onboarding Applications and Referral Relationships ──────────────

-- Add referral_code column to organizations
ALTER TABLE organizations
    ADD COLUMN referral_code VARCHAR(32);

CREATE UNIQUE INDEX idx_organizations_referral_code
    ON organizations (referral_code)
    WHERE referral_code IS NOT NULL;

-- ── Onboarding Applications ────────────────────────────────────────────────────

CREATE TABLE onboarding_applications (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Company info
    company_name            VARCHAR(255) NOT NULL,
    trade_name              VARCHAR(255),
    cnpj                    VARCHAR(20)  NOT NULL,
    plan_code               VARCHAR(80)  NOT NULL,

    -- Owner info
    owner_name              VARCHAR(255) NOT NULL,
    owner_cpf               VARCHAR(14)  NOT NULL,
    owner_email             VARCHAR(255) NOT NULL,
    owner_phone             VARCHAR(20)  NOT NULL,

    -- Address
    cep                     VARCHAR(10),
    street                  VARCHAR(255),
    number                  VARCHAR(20),
    complement              VARCHAR(100),
    neighborhood            VARCHAR(100),
    city                    VARCHAR(100),
    state                   CHAR(2),

    -- Workflow
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    referral_code           VARCHAR(32),
    rejection_reason        TEXT,
    approved_organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    approved_at             TIMESTAMP,
    rejected_at             TIMESTAMP,

    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_onboarding_applications_status
    ON onboarding_applications (status);

CREATE INDEX idx_onboarding_applications_company_name
    ON onboarding_applications (company_name);

CREATE INDEX idx_onboarding_applications_owner_email
    ON onboarding_applications (owner_email);

CREATE INDEX idx_onboarding_applications_created_at
    ON onboarding_applications (created_at);

-- ── Referral Relationships ─────────────────────────────────────────────────────

CREATE TABLE referral_relationships (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    referrer_organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    referred_organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    onboarding_application_id UUID        REFERENCES onboarding_applications(id) ON DELETE SET NULL,

    status                   VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    commission_rate          NUMERIC(5,4) NOT NULL DEFAULT 0.08,
    commission_amount_brl    NUMERIC(10,2),

    approved_at              TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_referral_relationships_referred_org
        UNIQUE (referred_organization_id)
);

CREATE INDEX idx_referral_relationships_referrer_org
    ON referral_relationships (referrer_organization_id);

CREATE INDEX idx_referral_relationships_status
    ON referral_relationships (status);
