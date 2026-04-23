ALTER TABLE plans
    ADD COLUMN billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN abacate_product_id VARCHAR(100),
    ADD COLUMN abacate_product_external_id VARCHAR(120);

ALTER TABLE subscriptions
    ADD COLUMN payment_provider VARCHAR(30),
    ADD COLUMN abacate_customer_id VARCHAR(100),
    ADD COLUMN abacate_subscription_id VARCHAR(100),
    ADD COLUMN abacate_checkout_id VARCHAR(100),
    ADD COLUMN abacate_checkout_url TEXT,
    ADD COLUMN abacate_checkout_status VARCHAR(30);

CREATE TABLE billing_history_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    provider_event_id VARCHAR(120),
    provider_payment_id VARCHAR(120),
    provider_checkout_id VARCHAR(120),
    provider_subscription_id VARCHAR(120),
    external_reference VARCHAR(160),
    description VARCHAR(255) NOT NULL,
    amount_brl NUMERIC(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    billed_at DATE NOT NULL,
    issued_at DATE NOT NULL,
    invoice_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_billing_history_entries_provider_event UNIQUE (provider_event_id)
);

CREATE INDEX idx_billing_history_entries_subscription ON billing_history_entries(subscription_id);
CREATE INDEX idx_billing_history_entries_provider_subscription ON billing_history_entries(provider_subscription_id);
