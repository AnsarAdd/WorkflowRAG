package com.ansaradd.workflowragsrc.retrieval.repository;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface RetrievalSearchRepository {

  List<RetrievalHit> search(
      float[] queryEmbedding,
      String provider,
      String model,
      String modelRevision,
      int dimensions,
      double minScore,
      List<String> sourceIds,
      int limit
  );
}