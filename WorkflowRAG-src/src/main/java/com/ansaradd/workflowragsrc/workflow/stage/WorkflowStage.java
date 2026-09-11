package com.ansaradd.workflowragsrc.workflow.stage;

import com.ansaradd.workflowragsrc.workflow.model.Job;

public interface WorkflowStage {

  String id();

  void execute(Job job);
}