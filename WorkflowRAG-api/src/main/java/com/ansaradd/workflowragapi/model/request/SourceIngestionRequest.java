package com.ansaradd.workflowragapi.model.request;

import java.util.Map;

public record SourceIngestionRequest(
    String sourceId,
    String externalDocumentId,
    Map<String, String> parameters
) {

  public SourceIngestionRequest {
    if (sourceId == null || sourceId.isBlank()) {
      throw new IllegalArgumentException("sourceId must not be blank");
    }

    if (externalDocumentId == null || externalDocumentId.isBlank()) {
      throw new IllegalArgumentException("externalDocumentId must not be blank");
    }

    parameters = parameters == null
        ? Map.of()
        : Map.copyOf(parameters);
  }
}