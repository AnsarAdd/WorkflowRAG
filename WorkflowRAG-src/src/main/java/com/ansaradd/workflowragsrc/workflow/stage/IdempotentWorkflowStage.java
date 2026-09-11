package com.ansaradd.workflowragsrc.workflow.stage;

import com.ansaradd.workflowragsrc.workflow.model.Job;

public interface IdempotentWorkflowStage extends WorkflowStage {

  String fingerprint(Job job);

  boolean canReuse(
      Job job,
      String fingerprint
  );
}