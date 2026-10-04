package com.ansaradd.workflowragapi.model.response;

import com.ansaradd.workflowragapi.model.enums.ResolvedContextScope;
import java.util.List;
import java.util.UUID;

public record ResolvedContextResponse(
    UUID documentId,
    UUID documentVersionId,
    UUID sectionId,
    UUID originChunkId,
    String sourceId,
    String externalDocumentId,
    String sectionTitle,
    String content,
    double retrievalScore,
    ResolvedContextScope scope,
    List<UUID> includedChunkIds
) {

  public ResolvedContextResponse {
    includedChunkIds =
        List.copyOf(includedChunkIds);
  }
}