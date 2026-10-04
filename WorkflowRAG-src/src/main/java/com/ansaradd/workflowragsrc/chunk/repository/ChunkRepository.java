package com.ansaradd.workflowragsrc.chunk.repository;

import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChunkRepository {

  void replaceForVersion(
      UUID documentVersionId,
      String processingFingerprint,
      List<Chunk> chunks
  );

  boolean hasReusableChunking(
      UUID documentVersionId,
      String processingFingerprint
  );

  List<Chunk> findByDocumentVersionId(
      UUID documentVersionId
  );

  Optional<Chunk> findById(UUID id);

  List<Chunk> findNeighbors(
      UUID sectionId,
      int chunkIndex,
      int distance
  );
}