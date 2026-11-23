CREATE TABLE ai_token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    bot_session_id UUID NOT NULL REFERENCES bot_session(id),
    engine VARCHAR(20) NOT NULL,
    tokens_input INTEGER NOT NULL,
    tokens_output INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_token_engine CHECK (engine IN ('LEX', 'BEDROCK'))
);

CREATE INDEX idx_ai_token_org_month ON ai_token_usage(organization_id, occurred_at);
