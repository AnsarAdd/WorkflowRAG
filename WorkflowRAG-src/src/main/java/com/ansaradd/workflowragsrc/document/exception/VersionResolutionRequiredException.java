package com.ansaradd.workflowragsrc.document.exception;

import com.ansaradd.workflowragsrc.source.model.DocumentKey;

public class VersionResolutionRequiredException extends RuntimeException {

  public VersionResolutionRequiredException(DocumentKey documentKey) {
    super(
        "Version resolution is required for document: "
            + documentKey
    );
  }
}