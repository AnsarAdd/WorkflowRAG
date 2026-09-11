package com.ansaradd.workflowragsrc.document.exception;

import com.ansaradd.workflowragsrc.document.model.DocumentVersionStatus;
import java.util.UUID;

public class InvalidDocumentVersionStateException extends RuntimeException {

  public InvalidDocumentVersionStateException(
      UUID versionId,
      DocumentVersionStatus status,
      String operation
  ) {
    super(
        "Cannot " + operation
            + " document version " + versionId
            + " with status " + status
    );
  }
}