ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS ai_engine_tier VARCHAR(40) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS max_ai_tokens_per_month INTEGER NULL;

UPDATE plans
SET ai_engine_tier = 'NONE',
    max_ai_tokens_per_month = NULL
WHERE name LIKE 'Básico%' OR name LIKE 'Basico%';

UPDATE plans
SET ai_engine_tier = 'LEX_ONLY',
    max_ai_tokens_per_month = NULL
WHERE name LIKE 'Profissional%';

UPDATE plans
SET ai_engine_tier = 'LEX_BEDROCK_FALLBACK',
    max_ai_tokens_per_month = 500000
WHERE name LIKE 'Enterprise%';

ALTER TABLE plans
    DROP CONSTRAINT IF EXISTS chk_plan_ai_tier;

ALTER TABLE plans
    ADD CONSTRAINT chk_plan_ai_tier
    CHECK (ai_engine_tier IN ('NONE', 'LEX_ONLY', 'LEX_BEDROCK_FALLBACK'));
