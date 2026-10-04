package com.ansaradd.workflowragsrc.context.service;

import com.ansaradd.workflowragsrc.context.model.ContextExpansionMode;
import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.List;

public interface ContextResolver {

  List<ResolvedContext> resolve(
      List<RetrievalHit> hits,
      ContextExpansionStrategy strategy
  );
}