package com.ansaradd.workflowragsrc.source.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflowrag")
public record SourceProperties(
    List<SourceDefinition> sources
) {

  public SourceProperties {
    sources = sources == null
        ? List.of()
        : List.copyOf(sources);
  }
}