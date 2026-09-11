package com.ansaradd.workflowragsrc.retrieval.service;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface RetrievalService {

  List<RetrievalHit> search(
      String query,
      String sourceId,
      int limit
  );
}