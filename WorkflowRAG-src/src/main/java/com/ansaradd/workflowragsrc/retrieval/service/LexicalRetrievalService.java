package com.ansaradd.workflowragsrc.retrieval.service;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface LexicalRetrievalService {

  List<RetrievalHit> search(
      String query,
      List<String> sourceIds,
      int limit
  );
}