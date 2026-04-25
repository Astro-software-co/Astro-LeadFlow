-- ── Phase 13-01D: Referral Commission Records ─────────────────────────────────
-- Tracks first-payment commission events, one per referral relationship (idempotent).

CREATE TABLE IF NOT EXISTS commission_records (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    referral_relationship_id UUID         NOT NULL REFERENCES referral_relationships(id) ON DELETE CASCADE,
    billing_history_entry_id UUID         REFERENCES billing_history_entries(id) ON DELETE SET NULL,

    amount_brl               NUMERIC(10,2) NOT NULL,
    commission_rate          NUMERIC(5,4)  NOT NULL,
    tier                     VARCHAR(20)   NOT NULL,

    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- One commission record per referral relationship (first payment only)
    CONSTRAINT uq_commission_records_relationship UNIQUE (referral_relationship_id)
);

CREATE INDEX IF NOT EXISTS idx_commission_records_relationship
    ON commission_records (referral_relationship_id);
