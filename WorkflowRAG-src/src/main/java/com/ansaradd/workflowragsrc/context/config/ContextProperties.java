package com.ansaradd.workflowragsrc.context.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag.context")
public record ContextProperties(
    int smallSectionMaxChars,
    int neighborDistance
) {

  public ContextProperties {
    if (smallSectionMaxChars <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.context.small-section-max-chars "
              + "must be positive"
      );
    }

    if (neighborDistance < 0) {
      throw new IllegalArgumentException(
          "workflowrag.context.neighbor-distance "
              + "must not be negative"
      );
    }
  }
}