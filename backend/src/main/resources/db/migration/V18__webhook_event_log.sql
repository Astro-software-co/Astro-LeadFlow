CREATE TABLE webhook_event_log (
    id BIGSERIAL PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    source VARCHAR(40) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    http_status INTEGER NOT NULL,
    correlation_id VARCHAR(64),
    duration_ms INTEGER,
    error_message TEXT,
    CONSTRAINT chk_webhook_source CHECK (source IN ('META', 'ABACATEPAY', 'WEBMOTORS', 'MERCADOLIVRE', 'SCRAPER')),
    CONSTRAINT chk_webhook_outcome CHECK (outcome IN ('SUCCESS', 'SIGNATURE_FAIL', 'RATE_LIMITED', 'PROCESSING_ERROR', 'REPLAY_BLOCKED'))
);

CREATE INDEX idx_webhook_log_source_time ON webhook_event_log(source, received_at DESC);
CREATE INDEX idx_webhook_log_outcome ON webhook_event_log(outcome) WHERE outcome != 'SUCCESS';
