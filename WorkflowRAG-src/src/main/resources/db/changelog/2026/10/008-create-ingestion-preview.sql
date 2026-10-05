CREATE TABLE ingestion_preview (
    id UUID PRIMARY KEY,
    format VARCHAR(32) NOT NULL,
    content BYTEA,
    force_reindex BOOLEAN NOT NULL,
    report JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    job_id UUID REFERENCES job(id) ON DELETE CASCADE
);

CREATE INDEX idx_ingestion_preview_expiry ON ingestion_preview(expires_at);

CREATE INDEX idx_chunk_embedding_fingerprint ON chunk_embedding(processing_fingerprint);
