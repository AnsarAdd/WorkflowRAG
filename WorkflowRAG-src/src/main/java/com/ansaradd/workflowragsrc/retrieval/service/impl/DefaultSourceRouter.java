package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.retrieval.config.SourceRoutingProperties;
import com.ansaradd.workflowragsrc.retrieval.service.SourceRouter;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultSourceRouter
    implements SourceRouter {

  /*
   * Description-only single token match is intentionally
   * not enough to restrict retrieval.
   */
  private static final int MIN_ROUTING_SCORE = 3;

  private final SourceRegistry sourceRegistry;
  private final SourceRoutingProperties properties;

  public DefaultSourceRouter(
      SourceRegistry sourceRegistry,
      SourceRoutingProperties properties
  ) {
    this.sourceRegistry = sourceRegistry;
    this.properties = properties;
  }

  @Override
  public List<String> route(
      String query
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );

    if (!properties.enabled()) {
      return List.of();
    }

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    Set<String> queryTokens =
        tokenize(
            normalizedQuery
        );

    if (queryTokens.isEmpty()) {
      return List.of();
    }

    return sourceRegistry.findAll()
        .stream()
        .map(
            source ->
                new ScoredSource(
                    source.id(),
                    score(
                        queryTokens,
                        source
                    )
                )
        )
        .filter(
            scored ->
                scored.score()
                    >= MIN_ROUTING_SCORE
        )
        .sorted(
            Comparator
                .comparingInt(
                    ScoredSource::score
                )
                .reversed()
                .thenComparing(
                    ScoredSource::sourceId
                )
        )
        .limit(
            properties.maxSources()
        )
        .map(
            ScoredSource::sourceId
        )
        .toList();
  }

  private int score(
      Set<String> queryTokens,
      Source source
  ) {
    int score = 0;

    score += matchingScore(
        queryTokens,
        source.id(),
        4
    );

    score += matchingScore(
        queryTokens,
        source.name(),
        3
    );

    score += matchingScore(
        queryTokens,
        source.description(),
        1
    );

    if (source.type() != null) {
      score += matchingScore(
          queryTokens,
          source.type().name(),
          2
      );
    }

    return score;
  }

  private int matchingScore(
      Set<String> queryTokens,
      String text,
      int weight
  ) {
    if (text == null
        || text.isBlank()) {
      return 0;
    }

    Set<String> textTokens =
        tokenize(text);

    int matches = 0;

    for (String token : queryTokens) {
      if (textTokens.contains(token)) {
        matches++;
      }
    }

    return matches * weight;
  }

  private Set<String> tokenize(
      String text
  ) {
    if (text == null
        || text.isBlank()) {
      return Set.of();
    }

    /*
     * processPayment -> process Payment
     * PaymentService -> Payment Service
     */
    String expanded =
        text.replaceAll(
            "([\\p{Ll}\\d])([\\p{Lu}])",
            "$1 $2"
        );

    String normalized =
        expanded.toLowerCase(
            Locale.ROOT
        );

    Set<String> result =
        new LinkedHashSet<>();

    Arrays.stream(
            normalized.split(
                "[^\\p{L}\\p{N}]+"
            )
        )
        .filter(
            token ->
                token.length()
                    >= properties.minTokenLength()
        )
        .forEach(result::add);

    return Set.copyOf(result);
  }

  private record ScoredSource(
      String sourceId,
      int score
  ) {
  }
}