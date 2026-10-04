package com.ansaradd.workflowragsrc.retrieval.service;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface RrfFusionService {

  List<RetrievalHit> fuse(
      List<RetrievalHit> semanticHits,
      List<RetrievalHit> lexicalHits,
      int limit
  );
}