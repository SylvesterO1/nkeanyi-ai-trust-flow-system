CREATE TABLE IF NOT EXISTS compliance_evidence (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    document_id VARCHAR(150),
    payment_id VARCHAR(150),
    evidence_type VARCHAR(80) NOT NULL,
    source_service VARCHAR(120) NOT NULL,
    source_topic VARCHAR(180),
    event_key VARCHAR(180),
    summary VARCHAR(500) NOT NULL,
    risk_level VARCHAR(50),
    decision VARCHAR(100),
    actor VARCHAR(150),
    payload_hash VARCHAR(128),
    evidence_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    immutable_record BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_evidence_tenant_id
    ON compliance_evidence (tenant_id);

CREATE INDEX IF NOT EXISTS idx_evidence_correlation_id
    ON compliance_evidence (correlation_id);

CREATE INDEX IF NOT EXISTS idx_evidence_document_id
    ON compliance_evidence (document_id);

CREATE INDEX IF NOT EXISTS idx_evidence_payment_id
    ON compliance_evidence (payment_id);

CREATE INDEX IF NOT EXISTS idx_evidence_type
    ON compliance_evidence (evidence_type);

CREATE INDEX IF NOT EXISTS idx_evidence_created_at
    ON compliance_evidence (created_at);
