package com.ansaradd.workflowragsrc.workflow.service;

import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;

public interface PipelineRegistry {

  PipelineTemplate getRequired(String pipelineId);
}