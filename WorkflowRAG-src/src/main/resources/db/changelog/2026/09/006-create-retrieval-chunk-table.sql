CREATE TABLE retrieval_chunk
(
    chunk_id       UUID        NOT NULL,
    search_vector  TSVECTOR    NOT NULL,
    indexed_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_retrieval_chunk
        PRIMARY KEY (chunk_id),

    CONSTRAINT fk_retrieval_chunk_chunk
        FOREIGN KEY (chunk_id)
            REFERENCES document_chunk (id)
            ON DELETE CASCADE
);


CREATE INDEX ix_retrieval_chunk_search_vector
    ON retrieval_chunk
    USING GIN (search_vector);