package com.ansaradd.workflowragsrc.preflight.exception;

public class PreviewStateException extends RuntimeException {
  private final String code;

  public PreviewStateException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
