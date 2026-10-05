CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document
(
    id                   UUID         NOT NULL,
    source_id            VARCHAR(255) NOT NULL,
    external_document_id VARCHAR(1024) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document
        PRIMARY KEY (id),

    CONSTRAINT uq_document_source_external_id
        UNIQUE (source_id, external_document_id)
);


CREATE TABLE document_version
(
    id             UUID         NOT NULL,
    document_id    UUID         NOT NULL,
    version_number BIGINT       NOT NULL,

    format         VARCHAR(32)  NOT NULL,
    content_hash   VARCHAR(64)  NOT NULL,
    content        BYTEA        NOT NULL,

    status         VARCHAR(32)  NOT NULL,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at   TIMESTAMPTZ,

    CONSTRAINT pk_document_version
        PRIMARY KEY (id),

    CONSTRAINT fk_document_version_document
        FOREIGN KEY (document_id)
            REFERENCES document (id),

    CONSTRAINT uq_document_version_number
        UNIQUE (document_id, version_number),

    CONSTRAINT chk_document_version_number
        CHECK (version_number > 0),

    CONSTRAINT chk_document_version_status
        CHECK (
            status IN (
                       'BUILDING',
                       'ACTIVE',
                       'INACTIVE',
                       'FAILED'
                )
            ),

    CONSTRAINT chk_document_version_content_hash
        CHECK (
            content_hash ~ '^[0-9a-f]{64}$'
),

    CONSTRAINT chk_document_version_activation
        CHECK (
            (
                status IN ('ACTIVE', 'INACTIVE')
                AND activated_at IS NOT NULL
            )
            OR
            (
                status IN ('BUILDING', 'FAILED')
                AND activated_at IS NULL
            )
        )
);


CREATE UNIQUE INDEX uq_document_version_active
    ON document_version (document_id)
    WHERE status = 'ACTIVE';


CREATE UNIQUE INDEX uq_document_version_building
    ON document_version (document_id)
    WHERE status = 'BUILDING';