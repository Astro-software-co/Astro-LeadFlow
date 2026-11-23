-- ── Phase 13-01D: Onboarding Contract Parity ──────────────────────────────────
-- Adds extended owner/dealership profile fields and marketplace intent to onboarding_applications.

ALTER TABLE onboarding_applications
    ADD COLUMN IF NOT EXISTS owner_role          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dealership_segment  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS branch_count        INTEGER,
    ADD COLUMN IF NOT EXISTS employee_count      INTEGER,
    ADD COLUMN IF NOT EXISTS stock_count         INTEGER,
    ADD COLUMN IF NOT EXISTS monthly_leads_avg   INTEGER,
    ADD COLUMN IF NOT EXISTS market_years        INTEGER,
    ADD COLUMN IF NOT EXISTS website             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS intended_marketplaces TEXT,
    ADD COLUMN IF NOT EXISTS other_marketplaces  TEXT;

ALTER TABLE onboarding_applications
    ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL';

UPDATE onboarding_applications
SET status = 'PENDING_APPROVAL'
WHERE status = 'PENDING';
