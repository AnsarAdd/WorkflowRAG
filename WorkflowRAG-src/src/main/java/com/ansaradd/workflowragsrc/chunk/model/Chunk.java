package com.ansaradd.workflowragsrc.chunk.model;

import java.util.Objects;
import java.util.UUID;

public record Chunk(
    UUID id,
    UUID sectionId,
    int chunkIndex,
    String content,
    String contentHash,
    boolean splitSection
) {

  public Chunk {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(sectionId, "sectionId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(contentHash, "contentHash must not be null");

    if (chunkIndex < 0) {
      throw new IllegalArgumentException(
          "chunkIndex must not be negative"
      );
    }
  }
}