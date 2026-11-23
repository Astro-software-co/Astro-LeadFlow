ALTER TABLE plans
    RENAME COLUMN monthly_price_brl TO price_brl;

ALTER TABLE plans
    DROP CONSTRAINT IF EXISTS plans_name_key;

ALTER TABLE plans
    ADD COLUMN code VARCHAR(80),
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE plans
SET
    code = CASE
        WHEN name = 'Básico' AND billing_cycle = 'MONTHLY' THEN 'BASICO_MONTHLY'
        WHEN name = 'Profissional' AND billing_cycle = 'MONTHLY' THEN 'PROFISSIONAL_MONTHLY'
        WHEN name = 'Enterprise' AND billing_cycle = 'MONTHLY' THEN 'ENTERPRISE_MONTHLY'
        ELSE UPPER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '_', 'g')) || '_' || billing_cycle
    END,
    description = CASE
        WHEN name = 'Básico' THEN 'Plano Basico para equipes em crescimento.'
        WHEN name = 'Profissional' THEN 'Plano Profissional para operacoes com mais volume.'
        WHEN name = 'Enterprise' THEN 'Plano Enterprise para estruturas com alta escala.'
        ELSE 'Plano de assinatura Astro LeadFlow.'
    END,
    sort_order = CASE
        WHEN name = 'Básico' THEN 10
        WHEN name = 'Profissional' THEN 20
        WHEN name = 'Enterprise' THEN 30
        ELSE 999
    END;

ALTER TABLE plans
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN description SET NOT NULL;

ALTER TABLE plans
    ADD CONSTRAINT plans_code_key UNIQUE (code);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'BASICO_SEMIANNUALLY',
    'Básico',
    'Plano Basico para equipes em crescimento.',
    804.60,
    1000,
    1,
    'SEMIANNUALLY',
    10,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'BASICO_SEMIANNUALLY'
);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'BASICO_ANNUALLY',
    'Básico',
    'Plano Basico para equipes em crescimento.',
    1430.40,
    1000,
    1,
    'ANNUALLY',
    10,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'BASICO_ANNUALLY'
);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'PROFISSIONAL_SEMIANNUALLY',
    'Profissional',
    'Plano Profissional para operacoes com mais volume.',
    1614.60,
    3000,
    3,
    'SEMIANNUALLY',
    20,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'PROFISSIONAL_SEMIANNUALLY'
);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'PROFISSIONAL_ANNUALLY',
    'Profissional',
    'Plano Profissional para operacoes com mais volume.',
    2870.40,
    3000,
    3,
    'ANNUALLY',
    20,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'PROFISSIONAL_ANNUALLY'
);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'ENTERPRISE_SEMIANNUALLY',
    'Enterprise',
    'Plano Enterprise para estruturas com alta escala.',
    3234.60,
    10000,
    10,
    'SEMIANNUALLY',
    30,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'ENTERPRISE_SEMIANNUALLY'
);

INSERT INTO plans (
    id,
    code,
    name,
    description,
    price_brl,
    message_limit,
    marketplace_limit,
    billing_cycle,
    sort_order,
    created_at
)
SELECT
    gen_random_uuid(),
    'ENTERPRISE_ANNUALLY',
    'Enterprise',
    'Plano Enterprise para estruturas com alta escala.',
    5750.40,
    10000,
    10,
    'ANNUALLY',
    30,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM plans WHERE code = 'ENTERPRISE_ANNUALLY'
);
