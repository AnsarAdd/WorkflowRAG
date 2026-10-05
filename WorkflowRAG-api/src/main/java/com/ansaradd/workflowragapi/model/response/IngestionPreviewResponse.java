package com.ansaradd.workflowragapi.model.response;

import java.time.Instant;
import java.util.UUID;

public record IngestionPreviewResponse(
    UUID id,
    String sourceId,
    String externalDocumentId,
    String contentHash,
    UUID baselineVersionId,
    String status,
    boolean canConfirm,
    Instant expiresAt,
    PreviewChangeSummary changes,
    PreviewSimilarityReport similarity,
    UUID jobId
) {
  public IngestionPreviewResponse withState(String state, boolean confirmable, UUID job) {
    return new IngestionPreviewResponse(id, sourceId, externalDocumentId, contentHash,
        baselineVersionId, state, confirmable, expiresAt, changes, similarity, job);
  }
}
