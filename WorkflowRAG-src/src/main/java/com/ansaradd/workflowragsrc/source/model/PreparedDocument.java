package com.ansaradd.workflowragsrc.source.model;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import java.util.Map;
import java.util.Objects;

public record PreparedDocument(
    DocumentKey documentKey,
    DocumentFormat format,
    byte[] content,
    String contentHash,
    Map<String, Object> metadata
) {

  public PreparedDocument {
    Objects.requireNonNull(
        documentKey,
        "documentKey must not be null"
    );

    Objects.requireNonNull(
        format,
        "format must not be null"
    );

    Objects.requireNonNull(
        content,
        "content must not be null"
    );

    Objects.requireNonNull(
        contentHash,
        "contentHash must not be null"
    );

    content = content.clone();

    metadata = metadata == null
        ? Map.of()
        : Map.copyOf(metadata);
  }

  @Override
  public byte[] content() {
    return content.clone();
  }
}