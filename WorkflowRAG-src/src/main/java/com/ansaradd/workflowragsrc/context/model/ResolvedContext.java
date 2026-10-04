package com.ansaradd.workflowragsrc.context.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ResolvedContext(
    UUID documentId,
    UUID documentVersionId,
    UUID sectionId,
    UUID originChunkId,
    String sourceId,
    String externalDocumentId,
    String sectionTitle,
    String content,
    double retrievalScore,
    Scope scope,
    List<UUID> includedChunkIds
) {

  public ResolvedContext {
    Objects.requireNonNull(
        documentId,
        "documentId must not be null"
    );
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );
    Objects.requireNonNull(
        sectionId,
        "sectionId must not be null"
    );
    Objects.requireNonNull(
        originChunkId,
        "originChunkId must not be null"
    );
    Objects.requireNonNull(
        content,
        "content must not be null"
    );
    Objects.requireNonNull(
        scope,
        "scope must not be null"
    );
    Objects.requireNonNull(
        includedChunkIds,
        "includedChunkIds must not be null"
    );

    includedChunkIds =
        List.copyOf(includedChunkIds);
  }

  public enum Scope {
    CHUNK,
    SECTION,
    WINDOW
  }
}