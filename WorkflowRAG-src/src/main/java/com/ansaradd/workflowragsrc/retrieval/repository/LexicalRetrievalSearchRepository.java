package com.ansaradd.workflowragsrc.retrieval.repository;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface LexicalRetrievalSearchRepository {

  List<RetrievalHit> search(
      String query,
      List<String> sourceIds,
      int limit
  );
}