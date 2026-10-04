package com.ansaradd.workflowragsrc.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
    prefix = "workflowrag.source-routing"
)
public record SourceRoutingProperties(
    boolean enabled,
    int maxSources,
    int minTokenLength
) {

  public SourceRoutingProperties {
    if (maxSources <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.source-routing.max-sources "
              + "must be positive"
      );
    }

    if (minTokenLength <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.source-routing.min-token-length "
              + "must be positive"
      );
    }
  }
}