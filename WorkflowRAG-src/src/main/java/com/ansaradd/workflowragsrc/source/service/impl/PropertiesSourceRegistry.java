package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragsrc.source.config.SourceDefinition;
import com.ansaradd.workflowragsrc.source.config.SourceProperties;
import com.ansaradd.workflowragsrc.source.exception.UnknownSourceException;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PropertiesSourceRegistry
    implements SourceRegistry {

  private static final String DEFAULT_PIPELINE_ID =
      "default-ingestion";

  private final Map<String, Source> sources;

  public PropertiesSourceRegistry(
      SourceProperties properties
  ) {
    Map<String, Source> sourceMap =
        new HashMap<>();

    for (SourceDefinition definition
        : properties.sources()) {

      SourceUpdatePolicy updatePolicy =
          definition.updatePolicy() == null
              ? SourceUpdatePolicy.REQUIRE_VERSION_RESOLUTION
              : definition.updatePolicy();

      String pipelineId =
          definition.pipelineId() == null
              || definition.pipelineId().isBlank()
              ? DEFAULT_PIPELINE_ID
              : definition.pipelineId().strip();

      Source source =
          new Source(
              definition.id(),
              definition.type(),
              definition.name(),
              definition.description(),
              updatePolicy,
              pipelineId,
              definition.configuration()
          );

      Source previous =
          sourceMap.putIfAbsent(
              source.id(),
              source
          );

      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate source id: "
                + source.id()
        );
      }
    }

    this.sources =
        Map.copyOf(sourceMap);
  }

  @Override
  public List<Source> findAll() {
    return sources.values()
        .stream()
        .sorted(
            java.util.Comparator.comparing(
                Source::id
            )
        )
        .toList();
  }

  @Override
  public Source getRequired(
      String sourceId
  ) {
    Source source =
        sources.get(sourceId);

    if (source == null) {
      throw new UnknownSourceException(
          sourceId
      );
    }

    return source;
  }
}