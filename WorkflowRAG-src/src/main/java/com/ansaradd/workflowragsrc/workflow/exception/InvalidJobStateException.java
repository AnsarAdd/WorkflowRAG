package com.ansaradd.workflowragsrc.workflow.exception;

import com.ansaradd.workflowragsrc.workflow.model.JobStatus;
import java.util.UUID;

public class InvalidJobStateException extends RuntimeException {

  public InvalidJobStateException(
      UUID jobId,
      JobStatus status,
      String operation
  ) {
    super(
        "Cannot " + operation
            + " job " + jobId
            + " with status " + status
    );
  }
}