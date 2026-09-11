package com.ansaradd.workflowragsrc.workflow.exception;

public class PipelineNotFoundException extends RuntimeException {

  public PipelineNotFoundException(String pipelineId) {
    super("Pipeline not found: " + pipelineId);
  }
}