ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100);

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_outbox_events_tenant_id
    ON outbox_events (tenant_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_correlation_id
    ON outbox_events (correlation_id);
