package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.exception.PipelineNotFoundException;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import com.ansaradd.workflowragsrc.workflow.service.PipelineRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultPipelineRegistry implements PipelineRegistry {

  private final Map<String, PipelineTemplate> pipelines;

  public DefaultPipelineRegistry(
      List<PipelineTemplate> registeredPipelines
  ) {
    Map<String, PipelineTemplate> pipelineMap = new HashMap<>();

    for (PipelineTemplate pipeline : registeredPipelines) {
      PipelineTemplate previous =
          pipelineMap.putIfAbsent(
              pipeline.id(),
              pipeline
          );

      if (previous != null) {
        throw new IllegalStateException(
            "Multiple pipelines registered with id: "
                + pipeline.id()
        );
      }
    }

    this.pipelines = Map.copyOf(pipelineMap);
  }

  @Override
  public PipelineTemplate getRequired(String pipelineId) {
    PipelineTemplate pipeline =
        pipelines.get(pipelineId);

    if (pipeline == null) {
      throw new PipelineNotFoundException(pipelineId);
    }

    return pipeline;
  }
}