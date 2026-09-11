package com.ansaradd.workflowragsrc.document.exception;

import java.util.UUID;

public class DocumentVersionNotFoundException extends RuntimeException {

  public DocumentVersionNotFoundException(UUID versionId) {
    super("Document version not found: " + versionId);
  }
}