package com.ansaradd.workflowragsrc.workflow.service;

import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;

public interface WorkflowStageResolver {

  WorkflowStage resolve(String stageId);
}