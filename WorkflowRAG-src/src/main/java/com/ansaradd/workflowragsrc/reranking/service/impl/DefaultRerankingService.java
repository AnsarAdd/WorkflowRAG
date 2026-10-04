package com.ansaradd.workflowragsrc.reranking.service.impl;

import com.ansaradd.workflowragsrc.reranking.provider.RerankingProvider;
import com.ansaradd.workflowragsrc.reranking.service.RerankingService;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultRerankingService
    implements RerankingService {

  private final ObjectProvider<RerankingProvider>
      rerankingProvider;

  public DefaultRerankingService(
      ObjectProvider<RerankingProvider> rerankingProvider
  ) {
    this.rerankingProvider =
        rerankingProvider;
  }

  @Override
  public List<RetrievalHit> rerank(
      String query,
      List<RetrievalHit> candidates,
      int limit
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        candidates,
        "candidates must not be null"
    );

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    if (limit <= 0) {
      throw new IllegalArgumentException(
          "limit must be positive"
      );
    }

    if (candidates.isEmpty()) {
      return List.of();
    }

    RerankingProvider provider =
        rerankingProvider.getIfAvailable();

    if (provider == null) {
      throw new IllegalStateException(
          "Reranking is not enabled or configured"
      );
    }

    List<String> documents =
        candidates.stream()
            .map(RetrievalHit::content)
            .toList();

    List<Double> scores =
        provider.score(
            normalizedQuery,
            documents
        );

    if (scores.size()
        != candidates.size()) {
      throw new IllegalStateException(
          "Reranking provider returned "
              + scores.size()
              + " scores for "
              + candidates.size()
              + " candidates"
      );
    }

    List<ScoredHit> scoredHits =
        new ArrayList<>(
            candidates.size()
        );

    for (int index = 0;
        index < candidates.size();
        index++) {

      scoredHits.add(
          new ScoredHit(
              candidates.get(index),
              scores.get(index)
          )
      );
    }

    int resultLimit =
        Math.min(
            limit,
            candidates.size()
        );

    return scoredHits.stream()
        .sorted(
            Comparator
                .comparingDouble(
                    ScoredHit::score
                )
                .reversed()
                .thenComparing(
                    scoredHit ->
                        scoredHit.hit()
                            .chunkId()
                )
        )
        .limit(resultLimit)
        .map(this::toRetrievalHit)
        .toList();
  }

  private RetrievalHit toRetrievalHit(
      ScoredHit scoredHit
  ) {
    RetrievalHit hit =
        scoredHit.hit();

    return new RetrievalHit(
        hit.documentId(),
        hit.documentVersionId(),
        hit.sectionId(),
        hit.chunkId(),
        hit.sourceId(),
        hit.externalDocumentId(),
        hit.sectionTitle(),
        hit.content(),
        scoredHit.score()
    );
  }

  private record ScoredHit(
      RetrievalHit hit,
      double score
  ) {
  }
}