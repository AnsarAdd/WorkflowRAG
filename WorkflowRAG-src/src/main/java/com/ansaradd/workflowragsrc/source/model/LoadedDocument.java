package com.ansaradd.workflowragsrc.source.model;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import java.util.Map;
import java.util.Objects;

public record LoadedDocument(
    DocumentKey documentKey,
    DocumentFormat format,
    byte[] content,
    Map<String, Object> metadata
) {

  public LoadedDocument {
    Objects.requireNonNull(documentKey);
    Objects.requireNonNull(format);
    Objects.requireNonNull(content);

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