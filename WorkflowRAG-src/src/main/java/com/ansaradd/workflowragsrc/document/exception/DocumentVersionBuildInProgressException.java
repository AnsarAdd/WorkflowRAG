package com.ansaradd.workflowragsrc.document.exception;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import java.util.UUID;

public class DocumentVersionBuildInProgressException extends RuntimeException {

  public DocumentVersionBuildInProgressException(UUID documentId) {
    super("Document already has a BUILDING version: " + documentId);
  }
}