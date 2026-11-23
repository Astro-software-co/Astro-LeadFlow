-- Phase 14: consolidate roles and move active referral codes to users.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(6);

UPDATE users
SET role = 'MANAGER'
WHERE role = 'ADMIN';

UPDATE users
SET role = 'ASTRO_ADMIN'
WHERE role = 'SUPER_ADMIN';

UPDATE users
SET organization_id = NULL
WHERE role = 'ASTRO_ADMIN';

WITH numbered_users AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY created_at, id) AS sequence_number
    FROM users
    WHERE referral_code IS NULL
)
UPDATE users u
SET referral_code = LPAD((100000 + numbered_users.sequence_number)::text, 6, '0')
FROM numbered_users
WHERE u.id = numbered_users.id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_referral_code
    ON users (referral_code)
    WHERE referral_code IS NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_referral_code_six_digits
        CHECK (referral_code IS NULL OR referral_code ~ '^[0-9]{6}$');

ALTER TABLE users
    ADD CONSTRAINT chk_users_role_primary
        CHECK (role IN ('ASTRO_ADMIN', 'MANAGER', 'SELLER'));
