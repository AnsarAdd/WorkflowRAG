CREATE EXTENSION IF NOT EXISTS vector;


CREATE TABLE chunk_embedding
(
    id                     UUID          NOT NULL,
    chunk_id               UUID          NOT NULL,

    provider               VARCHAR(64)   NOT NULL,
    model                  VARCHAR(255)  NOT NULL,
    model_revision         VARCHAR(255),

    dimensions             INTEGER       NOT NULL,
    processing_fingerprint VARCHAR(64)   NOT NULL,

    embedding              vector(1024)  NOT NULL,

    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_chunk_embedding
        PRIMARY KEY (id),

    CONSTRAINT fk_chunk_embedding_chunk
        FOREIGN KEY (chunk_id)
            REFERENCES document_chunk (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_chunk_embedding_chunk
        UNIQUE (chunk_id),

    CONSTRAINT chk_chunk_embedding_dimensions
        CHECK (dimensions = 1024),

    CONSTRAINT chk_chunk_embedding_fingerprint
        CHECK (
            processing_fingerprint ~ '^[0-9a-f]{64}$'
)
    );