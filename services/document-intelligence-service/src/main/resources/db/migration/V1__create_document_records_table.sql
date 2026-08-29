CREATE TABLE IF NOT EXISTS document_records (
    id UUID PRIMARY KEY,
    document_id VARCHAR(180) UNIQUE NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    original_filename VARCHAR(500),
    content_type VARCHAR(180),
    extractor_used VARCHAR(180),
    confidence DOUBLE PRECISION,
    status VARCHAR(40) NOT NULL,
    extracted_text TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_records_document_id
    ON document_records (document_id);

CREATE INDEX IF NOT EXISTS idx_document_records_tenant_id
    ON document_records (tenant_id);

CREATE INDEX IF NOT EXISTS idx_document_records_correlation_id
    ON document_records (correlation_id);

CREATE INDEX IF NOT EXISTS idx_document_records_created_at
    ON document_records (created_at);
