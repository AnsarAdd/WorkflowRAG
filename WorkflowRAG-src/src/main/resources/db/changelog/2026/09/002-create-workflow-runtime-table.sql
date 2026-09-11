CREATE TABLE job
(
    id                  UUID         NOT NULL,
    type                VARCHAR(32)  NOT NULL,
    pipeline_id         VARCHAR(255) NOT NULL,

    source_id           VARCHAR(255) NOT NULL,
    document_id         UUID         NOT NULL,
    document_version_id UUID         NOT NULL,

    status              VARCHAR(32)  NOT NULL,
    current_stage       VARCHAR(255),

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,

    error               TEXT,

    CONSTRAINT pk_job
        PRIMARY KEY (id),

    CONSTRAINT fk_job_document
        FOREIGN KEY (document_id)
            REFERENCES document (id),

    CONSTRAINT fk_job_document_version
        FOREIGN KEY (document_version_id)
            REFERENCES document_version (id),

    CONSTRAINT chk_job_type
        CHECK (
            type IN (
                'INGESTION'
                )
            ),

    CONSTRAINT chk_job_status
        CHECK (
            status IN (
                       'PENDING',
                       'RUNNING',
                       'COMPLETED',
                       'FAILED'
                )
            )
);


CREATE TABLE job_step
(
    id               UUID         NOT NULL,
    job_id           UUID         NOT NULL,

    step_order       INTEGER      NOT NULL,
    stage            VARCHAR(255) NOT NULL,
    status           VARCHAR(32)  NOT NULL,

    attempt          INTEGER      NOT NULL DEFAULT 0,

    fingerprint      VARCHAR(255),
    output_reference TEXT,

    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,

    error            TEXT,

    CONSTRAINT pk_job_step
        PRIMARY KEY (id),

    CONSTRAINT fk_job_step_job
        FOREIGN KEY (job_id)
            REFERENCES job (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_job_step_order
        UNIQUE (job_id, step_order),

    CONSTRAINT uq_job_step_stage
        UNIQUE (job_id, stage),

    CONSTRAINT chk_job_step_order
        CHECK (step_order >= 0),

    CONSTRAINT chk_job_step_status
        CHECK (
            status IN (
                       'PENDING',
                       'RUNNING',
                       'SUCCEEDED',
                       'FAILED',
                       'SKIPPED'
                )
            ),

    CONSTRAINT chk_job_step_attempt
        CHECK (attempt >= 0)
);


CREATE UNIQUE INDEX uq_job_active_document_version
    ON job (document_version_id)
    WHERE status IN ('PENDING', 'RUNNING');


CREATE INDEX ix_job_step_job_order
    ON job_step (job_id, step_order);