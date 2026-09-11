package com.ansaradd.workflowragsrc.document.model;

import java.util.UUID;
import java.util.Objects;

public record ActiveDocumentVersion(
    UUID id,
    UUID documentId,
    long versionNumber,
    String contentHash
) {

  public ActiveDocumentVersion {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(contentHash, "contentHash must not be null");

    if (versionNumber < 1) {
      throw new IllegalArgumentException(
          "versionNumber must be greater than 0"
      );
    }

    if (contentHash.isBlank()) {
      throw new IllegalArgumentException(
          "contentHash must not be blank"
      );
    }
  }
}