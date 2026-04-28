ALTER TABLE usage_logs
    ADD COLUMN IF NOT EXISTS metric_type VARCHAR(40) NOT NULL DEFAULT 'MESSAGE_SENT';

ALTER TABLE usage_logs
    DROP CONSTRAINT IF EXISTS usage_logs_organization_id_marketplace_name_period_month_period_year_key;

ALTER TABLE usage_logs
    DROP CONSTRAINT IF EXISTS usage_logs_metric_type_check;

ALTER TABLE usage_logs
    DROP CONSTRAINT IF EXISTS chk_usage_log_metric_type;

ALTER TABLE usage_logs
    ADD CONSTRAINT chk_usage_log_metric_type
    CHECK (metric_type IN ('LEAD_CREATED', 'LEAD_DUPLICATE', 'MESSAGE_SENT', 'AI_TOKENS_INPUT', 'AI_TOKENS_OUTPUT'));

ALTER TABLE usage_logs
    ADD CONSTRAINT usage_logs_org_marketplace_period_metric_key
    UNIQUE (organization_id, marketplace_name, period_month, period_year, metric_type);
