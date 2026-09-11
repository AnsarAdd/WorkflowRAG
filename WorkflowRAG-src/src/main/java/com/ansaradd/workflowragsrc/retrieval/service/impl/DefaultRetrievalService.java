package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
import com.ansaradd.workflowragsrc.retrieval.config.RetrievalProperties;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.repository.RetrievalSearchRepository;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultRetrievalService
    implements RetrievalService {

  private final EmbeddingProvider embeddingProvider;
  private final RetrievalSearchRepository searchRepository;
  private final RetrievalProperties properties;
  private final SourceRegistry sourceRegistry;

  public DefaultRetrievalService(
      EmbeddingProvider embeddingProvider,
      RetrievalSearchRepository searchRepository,
      RetrievalProperties properties, SourceRegistry sourceRegistry
  ) {
    this.embeddingProvider = embeddingProvider;
    this.searchRepository = searchRepository;
    this.properties = properties;
    this.sourceRegistry = sourceRegistry;
  }

  @Override
  public List<RetrievalHit> search(
      String query,
      String sourceId,
      int limit
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    String normalizedSourceId =
        normalizeSourceId(
            sourceId
        );

    if (normalizedSourceId != null) {
      sourceRegistry.getRequired(
          normalizedSourceId
      );
    }

    if (limit <= 0
        || limit > properties.maxLimit()) {
      throw new IllegalArgumentException(
          "limit must be between 1 and "
              + properties.maxLimit()
      );
    }

    List<float[]> embeddings =
        embeddingProvider.embed(
            List.of(normalizedQuery)
        );

    if (embeddings.size() != 1) {
      throw new IllegalStateException(
          "Embedding provider returned "
              + embeddings.size()
              + " embeddings for retrieval query"
      );
    }

    return searchRepository.search(
        embeddings.getFirst(),
        embeddingProvider.id(),
        embeddingProvider.model(),
        embeddingProvider.revision(),
        embeddingProvider.dimensions(),
        properties.minScore(),
        normalizedSourceId,
        limit
    );
  }

  private String normalizeSourceId(
      String sourceId
  ) {
    if (sourceId == null) {
      return null;
    }

    String normalized =
        sourceId.strip();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(
          "sourceId must not be blank"
      );
    }

    return normalized;
  }
}