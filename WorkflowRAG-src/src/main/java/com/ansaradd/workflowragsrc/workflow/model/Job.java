package com.ansaradd.workflowragsrc.workflow.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Job(
    UUID id,
    JobType type,
    String pipelineId,
    String sourceId,
    UUID documentId,
    UUID documentVersionId,
    JobStatus status,
    String currentStage,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    String error
) {

  public Job {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(pipelineId, "pipelineId must not be null");
    Objects.requireNonNull(sourceId, "sourceId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}