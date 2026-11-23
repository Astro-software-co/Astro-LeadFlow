CREATE TABLE application_error (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    level VARCHAR(10) NOT NULL,
    logger_name VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(64),
    organization_id UUID,
    message TEXT NOT NULL,
    stack_trace TEXT,
    request_path VARCHAR(500),
    request_method VARCHAR(10),
    user_id UUID,
    CONSTRAINT chk_app_error_level CHECK (level IN ('WARN', 'ERROR'))
);

CREATE INDEX idx_app_error_occurred ON application_error(occurred_at DESC);
CREATE INDEX idx_app_error_correlation ON application_error(correlation_id);
CREATE INDEX idx_app_error_level_occurred ON application_error(level, occurred_at DESC);
CREATE INDEX idx_app_error_org ON application_error(organization_id) WHERE organization_id IS NOT NULL;
