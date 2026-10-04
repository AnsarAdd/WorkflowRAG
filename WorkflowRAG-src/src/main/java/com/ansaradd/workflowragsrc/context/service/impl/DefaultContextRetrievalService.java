package com.ansaradd.workflowragsrc.context.service.impl;

import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
import com.ansaradd.workflowragsrc.context.service.ContextResolver;
import com.ansaradd.workflowragsrc.context.service.ContextRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalExecutionService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultContextRetrievalService
    implements ContextRetrievalService {

  private final RetrievalExecutionService retrievalExecutionService;
  private final ContextResolver contextResolver;

  public DefaultContextRetrievalService(
      RetrievalExecutionService retrievalExecutionService,
      ContextResolver contextResolver
  ) {
    this.retrievalExecutionService =
        retrievalExecutionService;
    this.contextResolver =
        contextResolver;
  }

  @Override
  public List<ResolvedContext> retrieve(
      String query,
      String sourceId,
      int limit,
      RetrievalProfile profile,
      ContextExpansionStrategy strategy
  ) {
    Objects.requireNonNull(
        profile,
        "profile must not be null"
    );
    Objects.requireNonNull(
        strategy,
        "strategy must not be null"
    );

    List<RetrievalHit> hits =
        retrievalExecutionService.search(
            query,
            sourceId,
            limit,
            profile
        );

    if (hits.isEmpty()) {
      return List.of();
    }

    return contextResolver.resolve(
        hits,
        strategy
    );
  }
}