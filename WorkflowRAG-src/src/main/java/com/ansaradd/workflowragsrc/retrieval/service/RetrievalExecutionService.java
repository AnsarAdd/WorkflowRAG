package com.ansaradd.workflowragsrc.retrieval.service;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import java.util.List;

public interface RetrievalExecutionService {

  List<RetrievalHit> search(
      String query,
      String sourceId,
      int limit,
      RetrievalProfile profile
  );
}