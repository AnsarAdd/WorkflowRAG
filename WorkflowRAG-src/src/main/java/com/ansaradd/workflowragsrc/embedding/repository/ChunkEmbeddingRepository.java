package com.ansaradd.workflowragsrc.embedding.repository;

import com.ansaradd.workflowragsrc.embedding.model.ChunkEmbedding;
import java.util.UUID;
import java.util.Map;

public interface ChunkEmbeddingRepository {

  Map<UUID, String> findFingerprintsByDocumentVersionId(
      UUID documentVersionId
  );

  boolean reuseFromActiveVersion(
      UUID targetChunkId,
      String processingFingerprint
  );

  void upsert(ChunkEmbedding embedding);
}