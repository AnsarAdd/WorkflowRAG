package com.ansaradd.workflowragapi.model.response;

import java.util.UUID;

public record PreviewSimilarityMatch(
    UUID documentVersionId,
    String sourceId,
    String externalDocumentId,
    String sectionTitle,
    String excerpt,
    String method,
    double score
) {
}
