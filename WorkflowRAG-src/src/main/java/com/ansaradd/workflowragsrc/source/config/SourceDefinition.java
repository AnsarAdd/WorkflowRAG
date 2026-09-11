package com.ansaradd.workflowragsrc.source.config;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import java.util.Map;

public record SourceDefinition(
    String id,
    SourceType type,
    String name,
    String description,
    SourceUpdatePolicy updatePolicy,
    String pipelineId,
    Map<String, Object> configuration
) {
}