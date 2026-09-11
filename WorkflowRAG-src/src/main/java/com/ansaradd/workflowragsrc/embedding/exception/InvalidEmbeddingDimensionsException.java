package com.ansaradd.workflowragsrc.embedding.exception;

public class InvalidEmbeddingDimensionsException
    extends RuntimeException {

  public InvalidEmbeddingDimensionsException(
      int expected,
      int actual
  ) {
    super(
        "Invalid embedding dimensions: expected="
            + expected
            + ", actual="
            + actual
    );
  }
}