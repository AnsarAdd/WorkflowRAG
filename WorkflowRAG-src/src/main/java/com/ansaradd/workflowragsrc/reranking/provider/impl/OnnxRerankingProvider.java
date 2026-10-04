package com.ansaradd.workflowragsrc.reranking.provider.impl;

import com.ansaradd.workflowragsrc.reranking.config.RerankingProperties;
import com.ansaradd.workflowragsrc.reranking.provider.RerankingProvider;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "workflowrag.reranking",
    name = "enabled",
    havingValue = "true"
)
public class OnnxRerankingProvider
    implements RerankingProvider {

  private static final String PROVIDER_ID =
      "onnx";

  private final String model;
  private final String revision;
  private final OnnxScoringModel scoringModel;

  public OnnxRerankingProvider(
      RerankingProperties properties
  ) {
    if (!PROVIDER_ID.equalsIgnoreCase(
        properties.provider()
    )) {
      throw new IllegalStateException(
          "Unsupported reranking provider: "
              + properties.provider()
      );
    }

    this.model =
        properties.model();

    this.revision =
        properties.revision();

    this.scoringModel =
        new OnnxScoringModel(
            properties.modelPath(),
            null,
            properties.tokenizerPath(),
            properties.maxLength(),
            properties.normalize()
        );
  }

  @Override
  public String id() {
    return PROVIDER_ID;
  }

  @Override
  public String model() {
    return model;
  }

  @Override
  public String revision() {
    return revision;
  }

  @Override
  public List<Double> score(
      String query,
      List<String> documents
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        documents,
        "documents must not be null"
    );

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    if (documents.isEmpty()) {
      return List.of();
    }

    List<TextSegment> segments =
        documents.stream()
            .map(this::toSegment)
            .toList();

    List<Double> scores =
        scoringModel
            .scoreAll(
                segments,
                normalizedQuery
            )
            .content();

    if (scores.size()
        != documents.size()) {
      throw new IllegalStateException(
          "Reranking provider returned "
              + scores.size()
              + " scores for "
              + documents.size()
              + " documents"
      );
    }

    for (Double score : scores) {
      if (score == null
          || !Double.isFinite(score)) {
        throw new IllegalStateException(
            "Reranking provider returned invalid score"
        );
      }
    }

    return List.copyOf(scores);
  }

  private TextSegment toSegment(
      String document
  ) {
    if (document == null
        || document.isBlank()) {
      throw new IllegalArgumentException(
          "reranking document must not be null or blank"
      );
    }

    return TextSegment.from(
        document
    );
  }

  @PreDestroy
  public void close() {
    scoringModel.close();
  }
}