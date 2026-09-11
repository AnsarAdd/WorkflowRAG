package com.ansaradd.workflowragsrc.retrieval.exception;

import java.util.UUID;

public class IncompleteEmbeddingIndexException
    extends RuntimeException {

  public IncompleteEmbeddingIndexException(
      UUID documentVersionId,
      long chunkCount,
      long embeddingCount
  ) {
    super(
        "Cannot index document version "
            + documentVersionId
            + ": chunks="
            + chunkCount
            + ", embeddings="
            + embeddingCount
    );
  }
}