package com.ansaradd.workflowragsrc.workflow.service;

import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import java.util.UUID;

public interface JobLifecycleService {

  Job create(
      JobType type,
      PipelineTemplate pipeline,
      String sourceId,
      UUID documentId,
      UUID documentVersionId
  );

  Job start(UUID jobId);

  void setCurrentStage(
      UUID jobId,
      String stage
  );

  void complete(UUID jobId);

  void fail(
      UUID jobId,
      String error
  );
}