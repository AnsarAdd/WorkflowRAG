CREATE TABLE document_chunk
(
    id                     UUID        NOT NULL,
    section_id             UUID        NOT NULL,
    chunk_index            INTEGER     NOT NULL,
    content                TEXT        NOT NULL,
    content_hash           VARCHAR(64) NOT NULL,
    processing_fingerprint VARCHAR(64) NOT NULL,
    split_section          BOOLEAN     NOT NULL,

    CONSTRAINT pk_document_chunk PRIMARY KEY (id),
    CONSTRAINT fk_document_chunk_section FOREIGN KEY (section_id) REFERENCES document_section (id) ON DELETE CASCADE,
    CONSTRAINT uq_document_chunk_index UNIQUE (section_id, chunk_index),
    CONSTRAINT chk_document_chunk_index
        CHECK (chunk_index >= 0),

    CONSTRAINT chk_document_chunk_content_hash
        CHECK (
            content_hash ~ '^[0-9a-f]{64}$'
) ,

    CONSTRAINT chk_document_chunk_processing_fingerprint
        CHECK (
            processing_fingerprint ~ '^[0-9a-f]{64}$'
        )
);