package com.ansaradd.workflowragsrc.document.exception;

import java.util.UUID;

public class DocumentSectionsNotFoundException
    extends RuntimeException {

  public DocumentSectionsNotFoundException(
      UUID documentVersionId
  ) {
    super(
        "No sections found for document version: "
            + documentVersionId
    );
  }
}