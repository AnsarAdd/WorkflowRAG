package com.ansaradd.workflowragsrc.source.model;

import java.util.Map;
import java.util.Objects;

public record SourceLoadContext(
    Source source,
    String externalDocumentId,
    Map<String, String> parameters
) {

  public SourceLoadContext {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(
        externalDocumentId,
        "externalDocumentId must not be null"
    );

    if (externalDocumentId.isBlank()) {
      throw new IllegalArgumentException(
          "externalDocumentId must not be blank"
      );
    }

    parameters = parameters == null
        ? Map.of()
        : Map.copyOf(parameters);
  }
}