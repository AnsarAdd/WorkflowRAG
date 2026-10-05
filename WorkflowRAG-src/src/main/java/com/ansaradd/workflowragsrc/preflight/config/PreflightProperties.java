package com.ansaradd.workflowragsrc.preflight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "workflowrag.preflight")
public record PreflightProperties(
    @DefaultValue("10485760") int maxBytes,
    @DefaultValue("2000") int maxSections,
    @DefaultValue("10000") int maxChunks,
    @DefaultValue("24") int ttlHours,
    @DefaultValue("3") int similaritySamples,
    @DefaultValue("30") int searchTimeoutSeconds
) {
  public PreflightProperties {
    if (maxBytes < 1 || maxBytes >= Integer.MAX_VALUE || maxSections < 1
        || maxChunks < 1 || ttlHours < 1 || similaritySamples < 1
        || similaritySamples > 10 || searchTimeoutSeconds < 1) {
      throw new IllegalArgumentException("Invalid preflight limits");
    }
  }
}
