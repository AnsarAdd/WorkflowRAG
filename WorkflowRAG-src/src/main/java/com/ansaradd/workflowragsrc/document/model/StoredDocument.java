package com.ansaradd.workflowragsrc.document.model;

import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import java.util.Objects;
import java.util.UUID;

public record StoredDocument(
    UUID id,
    DocumentKey documentKey
) {

  public StoredDocument {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(documentKey, "documentKey must not be null");
  }
}