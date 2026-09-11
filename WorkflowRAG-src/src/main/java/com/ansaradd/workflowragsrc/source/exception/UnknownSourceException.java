package com.ansaradd.workflowragsrc.source.exception;

public class UnknownSourceException extends RuntimeException {

  public UnknownSourceException(String sourceId) {
    super("Unknown source: " + sourceId);
  }
}
