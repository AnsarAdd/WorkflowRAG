package com.ansaradd.workflowragsrc.reranking.service;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface RerankingService {

  List<RetrievalHit> rerank(
      String query,
      List<RetrievalHit> candidates,
      int limit
  );
}