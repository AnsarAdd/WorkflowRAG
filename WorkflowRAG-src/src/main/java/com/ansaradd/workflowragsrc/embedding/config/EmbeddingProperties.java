package com.ansaradd.workflowragsrc.embedding.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag.embedding")
public record EmbeddingProperties(
    String provider,
    String baseUrl,
    String model,
    int dimensions,
    int batchSize,
    Duration timeout
) {

  public EmbeddingProperties {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.provider must not be blank"
      );
    }

    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.base-url must not be blank"
      );
    }

    if (model == null || model.isBlank()) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.model must not be blank"
      );
    }

    if (dimensions <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.dimensions must be positive"
      );
    }

    if (batchSize <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.batch-size must be positive"
      );
    }

    if (timeout == null
        || timeout.isZero()
        || timeout.isNegative()) {
      throw new IllegalArgumentException(
          "workflowrag.embedding.timeout must be positive"
      );
    }

    provider = provider.strip();
    baseUrl = baseUrl.strip();
    model = model.strip();
  }
}