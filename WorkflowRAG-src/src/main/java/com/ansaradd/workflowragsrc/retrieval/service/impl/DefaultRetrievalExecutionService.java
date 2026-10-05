package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.reranking.config.RerankingProperties;
import com.ansaradd.workflowragsrc.reranking.service.RerankingService;
import com.ansaradd.workflowragsrc.retrieval.config.RetrievalProperties;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import com.ansaradd.workflowragsrc.retrieval.service.HybridRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalExecutionService;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.SourceRouter;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultRetrievalExecutionService
    implements RetrievalExecutionService {

  private final RetrievalService semanticRetrievalService;
  private final HybridRetrievalService hybridRetrievalService;
  private final RerankingService rerankingService;
  private final RetrievalProperties retrievalProperties;
  private final RerankingProperties rerankingProperties;
  private final SourceRouter sourceRouter;
  private final SourceRegistry sourceRegistry;

  public DefaultRetrievalExecutionService(
      RetrievalService semanticRetrievalService,
      HybridRetrievalService hybridRetrievalService,
      RerankingService rerankingService,
      RetrievalProperties retrievalProperties,
      RerankingProperties rerankingProperties,
      SourceRouter sourceRouter,
      SourceRegistry sourceRegistry
  ) {
    this.semanticRetrievalService =
        semanticRetrievalService;
    this.hybridRetrievalService =
        hybridRetrievalService;
    this.rerankingService =
        rerankingService;
    this.retrievalProperties =
        retrievalProperties;
    this.rerankingProperties =
        rerankingProperties;
    this.sourceRouter =
        sourceRouter;
    this.sourceRegistry =
        sourceRegistry;
  }

  @Override
  public List<RetrievalHit> search(
      String query,
      String sourceId,
      int limit,
      RetrievalProfile profile
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        profile,
        "profile must not be null"
    );

    List<String> sourceIds =
        resolveSourceIds(
            query,
            sourceId
        );

    return switch (profile) {
      case SIMPLE ->
          semanticRetrievalService.search(
              query,
              sourceIds,
              limit
          );

      case HYBRID ->
          hybridRetrievalService.search(
              query,
              sourceIds,
              limit
          );

      case ADVANCED ->
          searchAdvanced(
              query,
              sourceIds,
              limit
          );
    };
  }

  private List<RetrievalHit> searchAdvanced(
      String query,
      List<String> sourceIds,
      int limit
  ) {
    if (!rerankingProperties.enabled()) {
      throw new com.ansaradd.workflowragsrc.reranking.exception.RerankingUnavailableException();
    }
    int candidateLimit =
        resolveCandidateLimit(
            limit
        );

    List<RetrievalHit> candidates =
        hybridRetrievalService.search(
            query,
            sourceIds,
            candidateLimit
        );

    return rerankingService.rerank(
        query,
        candidates,
        limit
    );
  }

  private List<String> resolveSourceIds(
      String query,
      String sourceId
  ) {
    if (sourceId != null) {
      String normalized =
          sourceId.strip();

      if (normalized.isEmpty()) {
        throw new IllegalArgumentException(
            "sourceId must not be blank"
        );
      }

      sourceRegistry.getRequired(
          normalized
      );

      return List.of(
          normalized
      );
    }

    return sourceRouter.route(
        query
    );
  }

  private int resolveCandidateLimit(
      int limit
  ) {
    long requested =
        (long) limit
            * rerankingProperties
            .candidateMultiplier();

    return (int) Math.min(
        retrievalProperties.maxLimit(),
        Math.max(
            limit,
            requested
        )
    );
  }
}