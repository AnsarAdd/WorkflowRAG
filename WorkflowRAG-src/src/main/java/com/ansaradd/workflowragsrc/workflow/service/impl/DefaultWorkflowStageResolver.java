package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.exception.WorkflowStageNotFoundException;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowStageResolver;
import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultWorkflowStageResolver implements WorkflowStageResolver {

  private final Map<String, WorkflowStage> stages;

  public DefaultWorkflowStageResolver(
      List<WorkflowStage> registeredStages
  ) {
    Map<String, WorkflowStage> stageMap = new HashMap<>();

    for (WorkflowStage stage : registeredStages) {
      Objects.requireNonNull(
          stage,
          "registered WorkflowStage must not be null"
      );

      String stageId = stage.id();

      if (stageId == null || stageId.isBlank()) {
        throw new IllegalStateException(
            "WorkflowStage id must not be null or blank: "
                + stage.getClass().getName()
        );
      }

      stageId = stageId.strip();

      WorkflowStage previous =
          stageMap.putIfAbsent(stageId, stage);

      if (previous != null) {
        throw new IllegalStateException(
            "Multiple WorkflowStages registered with id '"
                + stageId
                + "': "
                + previous.getClass().getName()
                + ", "
                + stage.getClass().getName()
        );
      }
    }

    this.stages = Map.copyOf(stageMap);
  }

  @Override
  public WorkflowStage resolve(String stageId) {
    if (stageId == null || stageId.isBlank()) {
      throw new WorkflowStageNotFoundException(stageId);
    }

    String normalizedStageId = stageId.strip();

    WorkflowStage stage =
        stages.get(normalizedStageId);

    if (stage == null) {
      throw new WorkflowStageNotFoundException(
          normalizedStageId
      );
    }

    return stage;
  }
}