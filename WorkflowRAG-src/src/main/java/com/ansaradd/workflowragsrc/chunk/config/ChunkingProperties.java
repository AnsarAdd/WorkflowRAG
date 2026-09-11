package com.ansaradd.workflowragsrc.chunk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag.chunking")
public record ChunkingProperties(
    int maxCharacters
) {

  public ChunkingProperties {
    if (maxCharacters <= 0) {
      throw new IllegalArgumentException(
          "workflowrag.chunking.max-characters must be positive"
      );
    }
  }
}