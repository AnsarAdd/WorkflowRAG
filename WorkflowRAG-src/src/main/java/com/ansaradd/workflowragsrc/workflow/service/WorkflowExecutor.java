package com.ansaradd.workflowragsrc.workflow.service;

import java.util.UUID;

public interface WorkflowExecutor {

  void execute(UUID jobId);
}