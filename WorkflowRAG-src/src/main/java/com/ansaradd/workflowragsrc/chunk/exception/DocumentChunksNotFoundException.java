package com.ansaradd.workflowragsrc.chunk.exception;

import java.util.UUID;

public class DocumentChunksNotFoundException extends RuntimeException {

  public DocumentChunksNotFoundException(UUID documentVersionId) {
    super(
        "No chunks found for document version: "
            + documentVersionId
    );
  }
}