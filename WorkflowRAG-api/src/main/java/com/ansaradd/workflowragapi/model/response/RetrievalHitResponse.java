package com.ansaradd.workflowragapi.model.response;

import java.util.UUID;

public record RetrievalHitResponse(
    UUID documentId,
    UUID documentVersionId,
    UUID sectionId,
    UUID chunkId,
    String sourceId,
    String externalDocumentId,
    String sectionTitle,
    String content,
    double score
) {
}