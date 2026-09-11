CREATE TABLE document_section
(
    id                     UUID          NOT NULL,
    document_version_id    UUID          NOT NULL,

    stable_key             VARCHAR(1024) NOT NULL,
    parent_id              UUID,
    section_order          INTEGER       NOT NULL,

    title                  TEXT,
    level                  INTEGER       NOT NULL,

    content                TEXT          NOT NULL,
    content_hash           VARCHAR(64)   NOT NULL,
    processing_fingerprint VARCHAR(64)   NOT NULL,

    CONSTRAINT pk_document_section
        PRIMARY KEY (id),

    CONSTRAINT fk_document_section_version
        FOREIGN KEY (document_version_id)
            REFERENCES document_version (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_document_section_parent
        FOREIGN KEY (parent_id)
            REFERENCES document_section (id),

    CONSTRAINT uq_document_section_stable_key
        UNIQUE (document_version_id, stable_key),

    CONSTRAINT uq_document_section_order
        UNIQUE (document_version_id, section_order),

    CONSTRAINT chk_document_section_order
        CHECK (section_order >= 0),

    CONSTRAINT chk_document_section_level
        CHECK (level >= 0),

    CONSTRAINT chk_document_section_content_hash
        CHECK (
            content_hash ~ '^[0-9a-f]{64}$'
),

    CONSTRAINT chk_document_section_processing_fingerprint
        CHECK (
            processing_fingerprint ~ '^[0-9a-f]{64}$'
        )
);


CREATE INDEX ix_document_section_parent
    ON document_section (
        document_version_id,
        parent_id,
        section_order
    );