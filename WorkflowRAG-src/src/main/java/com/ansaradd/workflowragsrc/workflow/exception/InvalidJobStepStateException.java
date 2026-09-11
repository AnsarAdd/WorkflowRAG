package com.ansaradd.workflowragsrc.workflow.exception;

import com.ansaradd.workflowragsrc.workflow.model.JobStepStatus;
import java.util.UUID;

public class InvalidJobStepStateException extends RuntimeException {

  public InvalidJobStepStateException(
      UUID stepId,
      JobStepStatus status,
      String operation
  ) {
    super(
        "Cannot " + operation
            + " job step " + stepId
            + " with status " + status
    );
  }
}