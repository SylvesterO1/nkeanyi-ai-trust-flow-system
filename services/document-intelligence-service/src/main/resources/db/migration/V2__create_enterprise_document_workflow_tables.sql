CREATE TABLE IF NOT EXISTS document_workflow_states (
    id UUID PRIMARY KEY,
    document_id VARCHAR(180) UNIQUE NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    workspace_id VARCHAR(160),
    correlation_id VARCHAR(150) NOT NULL,
    source_uri VARCHAR(800),
    checksum VARCHAR(128),
    owner_id VARCHAR(180),
    original_filename VARCHAR(500),
    content_type VARCHAR(180),
    document_type VARCHAR(80),
    sensitivity_label VARCHAR(80),
    access_policy VARCHAR(120),
    lifecycle_state VARCHAR(60) NOT NULL,
    review_status VARCHAR(60),
    reviewer_id VARCHAR(180),
    review_decision VARCHAR(80),
    decision_rationale TEXT,
    quarantine_reason TEXT,
    replacement_document_id VARCHAR(180),
    retention_policy VARCHAR(160),
    quality_gate_status VARCHAR(80),
    quality_gate_findings TEXT,
    extraction_confidence DOUBLE PRECISION,
    ocr_used BOOLEAN,
    extractor_used VARCHAR(180),
    published_at TIMESTAMP WITH TIME ZONE,
    deprecated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_workflow_states_tenant
    ON document_workflow_states (tenant_id);

CREATE INDEX IF NOT EXISTS idx_document_workflow_states_lifecycle
    ON document_workflow_states (lifecycle_state);

CREATE TABLE IF NOT EXISTS document_review_tasks (
    id UUID PRIMARY KEY,
    document_id VARCHAR(180) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    status VARCHAR(60) NOT NULL,
    reason TEXT,
    risk_level VARCHAR(40),
    assigned_role VARCHAR(120),
    reviewer_id VARCHAR(180),
    decision VARCHAR(80),
    rationale TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_document_review_tasks_document
    ON document_review_tasks (document_id);

CREATE INDEX IF NOT EXISTS idx_document_review_tasks_tenant_status
    ON document_review_tasks (tenant_id, status);

CREATE TABLE IF NOT EXISTS document_feedback_events (
    id UUID PRIMARY KEY,
    document_id VARCHAR(180) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(180) NOT NULL,
    category VARCHAR(120) NOT NULL,
    severity VARCHAR(40),
    message TEXT,
    status VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_feedback_events_document
    ON document_feedback_events (document_id);

CREATE TABLE IF NOT EXISTS document_audit_events (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    document_id VARCHAR(180),
    actor_id VARCHAR(180) NOT NULL,
    action VARCHAR(120) NOT NULL,
    result VARCHAR(80) NOT NULL,
    reason TEXT,
    correlation_id VARCHAR(150),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_audit_events_search
    ON document_audit_events (tenant_id, document_id, actor_id, action, timestamp);
