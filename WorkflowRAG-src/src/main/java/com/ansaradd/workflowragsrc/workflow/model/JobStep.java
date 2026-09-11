package com.ansaradd.workflowragsrc.workflow.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JobStep(
    UUID id,
    UUID jobId,
    int stepOrder,
    String stage,
    JobStepStatus status,
    int attempt,
    String fingerprint,
    String outputReference,
    Instant startedAt,
    Instant completedAt,
    String error
) {

  public JobStep {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(stage, "stage must not be null");
    Objects.requireNonNull(status, "status must not be null");

    if (stepOrder < 0) {
      throw new IllegalArgumentException(
          "stepOrder must not be negative"
      );
    }

    if (stage.isBlank()) {
      throw new IllegalArgumentException(
          "stage must not be blank"
      );
    }

    if (attempt < 0) {
      throw new IllegalArgumentException(
          "attempt must not be negative"
      );
    }
  }
}