package com.ansaradd.workflowragsrc.document.model;

import java.util.Objects;
import java.util.UUID;

public record StoredDocumentVersion(
    UUID id,
    UUID documentId,
    long versionNumber,
    DocumentVersionStatus status
) {

  public StoredDocumentVersion {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(status, "status must not be null");

    if (versionNumber < 1) {
      throw new IllegalArgumentException(
          "versionNumber must be greater than 0"
      );
    }
  }
}