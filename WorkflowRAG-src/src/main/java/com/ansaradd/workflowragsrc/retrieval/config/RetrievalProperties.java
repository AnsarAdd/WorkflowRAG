package com.ansaradd.workflowragsrc.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag.retrieval")
public record RetrievalProperties(
    int defaultLimit,
    int maxLimit,
    double minScore
) {

  public RetrievalProperties {
    if (defaultLimit <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.retrieval.default-limit must be positive"
      );
    }

    if (maxLimit <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.retrieval.max-limit must be positive"
      );
    }

    if (defaultLimit > maxLimit) {
      throw new IllegalArgumentException(
          "workflowrag.retrieval.default-limit must not exceed max-limit"
      );
    }

    if (minScore < -1.0 || minScore > 1.0) {
      throw new IllegalArgumentException(
          "workflowrag.retrieval.min-score must be between -1 and 1"
      );
    }
  }
}