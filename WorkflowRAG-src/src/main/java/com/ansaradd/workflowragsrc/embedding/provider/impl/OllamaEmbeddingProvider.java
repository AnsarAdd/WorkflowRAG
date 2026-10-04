package com.ansaradd.workflowragsrc.embedding.provider.impl;

import com.ansaradd.workflowragsrc.embedding.config.EmbeddingProperties;
import com.ansaradd.workflowragsrc.embedding.exception.InvalidEmbeddingDimensionsException;
import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
import com.ansaradd.workflowragsrc.embedding.provider.ollama.OllamaModelRevisionResolver;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OllamaEmbeddingProvider
    implements EmbeddingProvider {

  private static final String PROVIDER_ID = "ollama";

  private final String baseUrl;
  private final String model;
  private final int dimensions;
  private final int batchSize;
  private final Duration timeout;

  private final EmbeddingModel embeddingModel;
  private final OllamaModelRevisionResolver revisionResolver;

  private volatile String revision;

  public OllamaEmbeddingProvider(
      EmbeddingProperties properties,
      OllamaModelRevisionResolver revisionResolver
  ) {
    if (!PROVIDER_ID.equalsIgnoreCase(
        properties.provider()
    )) {
      throw new IllegalStateException(
          "Unsupported embedding provider: "
              + properties.provider()
      );
    }

    this.baseUrl = properties.baseUrl();
    this.model = properties.model();
    this.dimensions = properties.dimensions();
    this.batchSize = properties.batchSize();
    this.timeout = properties.timeout();
    this.revisionResolver = revisionResolver;

    this.embeddingModel =
        OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(model)
            .timeout(timeout)
            .build();
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
    String current =
        revision;

    if (current != null) {
      return current;
    }

    synchronized (this) {
      if (revision == null) {
        String resolved =
            revisionResolver.resolve(
                baseUrl,
                model,
                timeout
            );

        if (resolved.isBlank()) {
          throw new IllegalStateException(
              "Resolved Ollama model revision must not be blank: "
                  + model
          );
        }

        revision = resolved;
      }

      return revision;
    }
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public List<float[]> embed(
      List<String> texts
  ) {
    Objects.requireNonNull(
        texts,
        "texts must not be null"
    );

    if (texts.isEmpty()) {
      return List.of();
    }

    validateTexts(texts);

    List<float[]> result =
        new ArrayList<>(texts.size());

    for (int offset = 0;
        offset < texts.size();
        offset += batchSize) {

      int end =
          Math.min(
              offset + batchSize,
              texts.size()
          );

      List<TextSegment> segments =
          texts.subList(offset, end)
              .stream()
              .map(TextSegment::from)
              .toList();

      List<Embedding> embeddings =
          embeddingModel
              .embedAll(segments)
              .content();

      if (embeddings.size() != segments.size()) {
        throw new IllegalStateException(
            "Embedding provider returned "
                + embeddings.size()
                + " embeddings for "
                + segments.size()
                + " inputs"
        );
      }

      for (Embedding embedding : embeddings) {
        float[] vector =
            embedding.vector();

        validateDimensions(vector);

        result.add(vector.clone());
      }
    }

    return List.copyOf(result);
  }

  private void validateTexts(
      List<String> texts
  ) {
    for (String text : texts) {
      if (text == null || text.isBlank()) {
        throw new IllegalArgumentException(
            "embedding text must not be null or blank"
        );
      }
    }
  }

  private void validateDimensions(
      float[] vector
  ) {
    if (vector.length != dimensions) {
      throw new InvalidEmbeddingDimensionsException(
          dimensions,
          vector.length
      );
    }
  }
}