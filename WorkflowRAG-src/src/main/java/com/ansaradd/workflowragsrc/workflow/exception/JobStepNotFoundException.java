package com.ansaradd.workflowragsrc.workflow.exception;

import java.util.UUID;

public class JobStepNotFoundException extends RuntimeException {

  public JobStepNotFoundException(UUID stepId) {
    super("Job step not found: " + stepId);
  }
}