package com.ansaradd.workflowragsrc.source.exception;

public class SourceLoadException extends RuntimeException {

  public SourceLoadException(String message) {
    super(message);
  }

  public SourceLoadException(
      String message,
      Throwable cause
  ) {
    super(message, cause);
  }
}