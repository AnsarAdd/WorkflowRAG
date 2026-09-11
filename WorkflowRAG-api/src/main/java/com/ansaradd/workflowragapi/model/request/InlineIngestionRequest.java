package com.ansaradd.workflowragapi.model.request;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import java.util.Map;

public record InlineIngestionRequest(
    String sourceId,
    String externalDocumentId,
    DocumentFormat format,
    String content,
    Map<String, String> metadata
) {

  public InlineIngestionRequest {
    if (sourceId == null || sourceId.isBlank()) {
      throw new IllegalArgumentException("sourceId must not be blank");
    }

    if (externalDocumentId == null || externalDocumentId.isBlank()) {
      throw new IllegalArgumentException("externalDocumentId must not be blank");
    }

    if (format == null) {
      throw new IllegalArgumentException("format must not be null");
    }

    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content must not be blank");
    }

    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}