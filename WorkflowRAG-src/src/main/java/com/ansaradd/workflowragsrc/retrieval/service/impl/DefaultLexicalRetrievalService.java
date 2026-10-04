package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.retrieval.config.RetrievalProperties;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.repository.LexicalRetrievalSearchRepository;
import com.ansaradd.workflowragsrc.retrieval.service.LexicalRetrievalService;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultLexicalRetrievalService
    implements LexicalRetrievalService {

  private final LexicalRetrievalSearchRepository searchRepository;
  private final RetrievalProperties properties;
  private final SourceRegistry sourceRegistry;

  public DefaultLexicalRetrievalService(
      LexicalRetrievalSearchRepository searchRepository,
      RetrievalProperties properties,
      SourceRegistry sourceRegistry
  ) {
    this.searchRepository = searchRepository;
    this.properties = properties;
    this.sourceRegistry = sourceRegistry;
  }

  @Override
  public List<RetrievalHit> search(
      String query,
      List<String> sourceIds,
      int limit
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        sourceIds,
        "sourceIds must not be null"
    );

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    if (limit <= 0
        || limit > properties.maxLimit()) {
      throw new IllegalArgumentException(
          "limit must be between 1 and "
              + properties.maxLimit()
      );
    }

    return searchRepository.search(
        normalizedQuery,
        sourceIds,
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