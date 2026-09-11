package com.ansaradd.workflowragsrc.workflow.repository;

import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobStepRepository {

  List<JobStep> createPendingSteps(
      UUID jobId,
      List<String> stages
  );

  List<UUID> findRunningIds();

  List<JobStep> findByJobId(UUID jobId);

  Optional<JobStep> findById(UUID stepId);

  JobStep start(UUID stepId);

  void markSucceeded(
      UUID stepId,
      String fingerprint,
      String outputReference
  );

  void markFailed(
      UUID stepId,
      String error
  );

  void markSkipped(
      UUID stepId,
      String fingerprint,
      String outputReference
  );
}