package com.ansaradd.workflowragsrc.document.exception;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;

public class UnsupportedDocumentFormatException extends RuntimeException {

  public UnsupportedDocumentFormatException(DocumentFormat format) {
    super(" ");
  }
}