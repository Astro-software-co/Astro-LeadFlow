CREATE TABLE bot_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    lead_id UUID NOT NULL REFERENCES leads(id),
    engine_used VARCHAR(20) NOT NULL DEFAULT 'NONE',
    current_intent VARCHAR(80),
    slot_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fallback_count INTEGER NOT NULL DEFAULT 0,
    handoff_reason VARCHAR(80),
    handoff_at TIMESTAMPTZ,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bot_session_engine CHECK (engine_used IN ('NONE', 'TEMPLATE', 'LEX', 'BEDROCK')),
    CONSTRAINT chk_bot_session_status CHECK (status IN ('ACTIVE', 'HANDOFF', 'COMPLETED', 'ABANDONED'))
);

CREATE INDEX idx_bot_session_lead ON bot_session(lead_id);
CREATE INDEX idx_bot_session_org_status ON bot_session(organization_id, status);
CREATE UNIQUE INDEX uq_bot_session_active ON bot_session(lead_id) WHERE status = 'ACTIVE';
