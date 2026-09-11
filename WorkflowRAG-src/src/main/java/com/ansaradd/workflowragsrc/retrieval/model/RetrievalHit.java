package com.ansaradd.workflowragsrc.retrieval.model;

import java.util.UUID;

public record RetrievalHit(
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