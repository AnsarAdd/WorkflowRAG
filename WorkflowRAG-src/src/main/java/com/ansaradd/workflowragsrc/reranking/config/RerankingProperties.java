package com.ansaradd.workflowragsrc.reranking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag.reranking")
public record RerankingProperties(
    boolean enabled,
    String provider,
    String model,
    String revision,
    String modelPath,
    String tokenizerPath,
    int maxLength,
    boolean normalize,
    int candidateMultiplier
) {

  public RerankingProperties {
    if (candidateMultiplier <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.reranking.candidate-multiplier "
              + "must be positive"
      );
    }

    if (maxLength <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.reranking.max-length "
              + "must be positive"
      );
    }

    if (enabled) {
      if (provider == null || provider.isBlank()) {
        throw new IllegalArgumentException(
            "workflowrag.reranking.provider must not be blank"
        );
      }

      if (model == null || model.isBlank()) {
        throw new IllegalArgumentException(
            "workflowrag.reranking.model must not be blank"
        );
      }

      if (modelPath == null || modelPath.isBlank()) {
        throw new IllegalArgumentException(
            "workflowrag.reranking.model-path must not be blank"
        );
      }

      if (tokenizerPath == null
          || tokenizerPath.isBlank()) {
        throw new IllegalArgumentException(
            "workflowrag.reranking.tokenizer-path "
                + "must not be blank"
        );
      }

      provider = provider.strip();
      model = model.strip();
      modelPath = modelPath.strip();
      tokenizerPath = tokenizerPath.strip();

      revision =
          revision == null
              ? ""
              : revision.strip();
    }
  }
}