package com.ansaradd.workflowragsrc.workflow.exception;

import java.util.UUID;

public class WorkflowExecutionException extends RuntimeException {

  public WorkflowExecutionException(
      UUID jobId,
      String stage,
      Throwable cause
  ) {
    super(
        "Workflow job " + jobId
            + " failed at stage '" + stage + "'",
        cause
    );
  }
}