package com.ansaradd.workflowragsrc.context.service;

import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import java.util.List;

public interface ContextRetrievalService {

  List<ResolvedContext> retrieve(
      String query,
      String sourceId,
      int limit,
      RetrievalProfile profile,
      ContextExpansionStrategy strategy
  );
}