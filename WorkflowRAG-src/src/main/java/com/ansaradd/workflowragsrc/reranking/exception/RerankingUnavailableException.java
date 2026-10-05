package com.ansaradd.workflowragsrc.reranking.exception;

public class RerankingUnavailableException extends RuntimeException {
  public RerankingUnavailableException() {
    super("ADVANCED requires a configured reranking provider; use SIMPLE or HYBRID until it is enabled");
  }
}