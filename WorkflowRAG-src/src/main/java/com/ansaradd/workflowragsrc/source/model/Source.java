package com.ansaradd.workflowragsrc.source.model;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import java.util.Map;

public record Source(
    String id,
    SourceType type,
    String name,
    String description,
    SourceUpdatePolicy updatePolicy,
    String pipelineId,
    Map<String, Object> configuration
) {
}