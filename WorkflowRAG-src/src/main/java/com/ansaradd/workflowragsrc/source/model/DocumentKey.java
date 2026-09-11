package com.ansaradd.workflowragsrc.source.model;

public record DocumentKey(
    String sourceId,
    String externalDocumentId
) {

  public DocumentKey {
    if (sourceId == null || sourceId.isBlank()) {
      throw new IllegalArgumentException("sourceId must not be blank");
    }

    if (externalDocumentId == null || externalDocumentId.isBlank()) {
      throw new IllegalArgumentException(
          "externalDocumentId must not be blank"
      );
    }
  }
}