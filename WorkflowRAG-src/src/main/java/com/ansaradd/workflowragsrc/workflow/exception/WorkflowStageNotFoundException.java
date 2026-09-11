package com.ansaradd.workflowragsrc.workflow.exception;

public class WorkflowStageNotFoundException extends RuntimeException {

  public WorkflowStageNotFoundException(String stageId) {
    super("Workflow stage not found: " + stageId);
  }
}