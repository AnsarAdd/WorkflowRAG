package com.ansaradd.workflowragsrc.workflow.repository;

import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {

  Job create(
      JobType type,
      String pipelineId,
      String sourceId,
      UUID documentId,
      UUID documentVersionId
  );

  List<UUID> findRunningIds();

  Optional<Job> findById(UUID jobId);

  Job start(UUID jobId);

  void updateCurrentStage(
      UUID jobId,
      String stage
  );

  void markCompleted(UUID jobId);

  void markFailed(
      UUID jobId,
      String error
  );
}