package com.ansaradd.workflowragsrc.retrieval.evaluation;

import java.util.Objects;

public record RetrievalEvaluationCase(
    String name,
    String query,
    String searchSourceId,
    String expectedSourceId,
    String expectedExternalDocumentId
) {

  public RetrievalEvaluationCase {
    Objects.requireNonNull(
        name,
        "name must not be null"
    );
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        expectedSourceId,
        "expectedSourceId must not be null"
    );
    Objects.requireNonNull(
        expectedExternalDocumentId,
        "expectedExternalDocumentId must not be null"
    );

    if (name.isBlank()) {
      throw new IllegalArgumentException(
          "name must not be blank"
      );
    }

    if (query.isBlank()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    if (expectedSourceId.isBlank()) {
      throw new IllegalArgumentException(
          "expectedSourceId must not be blank"
      );
    }

    if (expectedExternalDocumentId.isBlank()) {
      throw new IllegalArgumentException(
          "expectedExternalDocumentId must not be blank"
      );
    }

    if (searchSourceId != null
        && searchSourceId.isBlank()) {
      throw new IllegalArgumentException(
          "searchSourceId must not be blank"
      );
    }
  }
}