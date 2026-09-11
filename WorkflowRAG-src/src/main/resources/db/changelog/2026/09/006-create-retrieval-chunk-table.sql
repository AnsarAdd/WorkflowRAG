CREATE TABLE retrieval_chunk
(
    chunk_id UUID NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_retrieval_chunk
        PRIMARY KEY (chunk_id),

    CONSTRAINT fk_retrieval_chunk_chunk
        FOREIGN KEY (chunk_id)
            REFERENCES document_chunk (id)
            ON DELETE CASCADE
);