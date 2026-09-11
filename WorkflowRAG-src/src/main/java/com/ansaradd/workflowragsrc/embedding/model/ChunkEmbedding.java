package com.ansaradd.workflowragsrc.embedding.model;

import java.util.Objects;
import java.util.UUID;

public record ChunkEmbedding(
    UUID id,
    UUID chunkId,
    String provider,
    String model,
    String modelRevision,
    int dimensions,
    String processingFingerprint,
    float[] vector
) {

  public ChunkEmbedding {
    Objects.requireNonNull(
        id,
        "id must not be null"
    );
    Objects.requireNonNull(
        chunkId,
        "chunkId must not be null"
    );
    Objects.requireNonNull(
        provider,
        "provider must not be null"
    );
    Objects.requireNonNull(
        model,
        "model must not be null"
    );
    Objects.requireNonNull(
        modelRevision,
        "modelRevision must not be null"
    );
    Objects.requireNonNull(
        processingFingerprint,
        "processingFingerprint must not be null"
    );
    Objects.requireNonNull(
        vector,
        "vector must not be null"
    );

    if (provider.isBlank()) {
      throw new IllegalArgumentException(
          "provider must not be blank"
      );
    }

    if (model.isBlank()) {
      throw new IllegalArgumentException(
          "model must not be blank"
      );
    }

    if (modelRevision.isBlank()) {
      throw new IllegalArgumentException(
          "modelRevision must not be blank"
      );
    }

    if (processingFingerprint.isBlank()) {
      throw new IllegalArgumentException(
          "processingFingerprint must not be blank"
      );
    }

    if (dimensions <= 0) {
      throw new IllegalArgumentException(
          "dimensions must be positive"
      );
    }

    if (vector.length != dimensions) {
      throw new IllegalArgumentException(
          "Embedding vector dimensions mismatch: expected="
              + dimensions
              + ", actual="
              + vector.length
      );
    }

    vector = vector.clone();
  }

  @Override
  public float[] vector() {
    return vector.clone();
  }
}