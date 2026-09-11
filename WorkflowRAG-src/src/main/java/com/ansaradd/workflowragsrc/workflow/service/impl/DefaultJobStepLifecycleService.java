package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.JobStepLifecycleService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultJobStepLifecycleService
    implements JobStepLifecycleService {

  private final JobStepRepository jobStepRepository;

  public DefaultJobStepLifecycleService(
      JobStepRepository jobStepRepository
  ) {
    this.jobStepRepository = jobStepRepository;
  }

  @Override
  public JobStep start(UUID stepId) {
    Objects.requireNonNull(stepId, "stepId must not be null");

    return jobStepRepository.start(stepId);
  }

  @Override
  public void succeed(
      UUID stepId,
      String fingerprint,
      String outputReference
  ) {
    Objects.requireNonNull(stepId, "stepId must not be null");

    jobStepRepository.markSucceeded(
        stepId,
        fingerprint,
        outputReference
    );
  }

  @Override
  public void fail(
      UUID stepId,
      String error
  ) {
    Objects.requireNonNull(stepId, "stepId must not be null");

    jobStepRepository.markFailed(
        stepId,
        error
    );
  }

  @Override
  public void skip(
      UUID stepId,
      String fingerprint,
      String outputReference
  ) {
    Objects.requireNonNull(stepId, "stepId must not be null");

    jobStepRepository.markSkipped(
        stepId,
        fingerprint,
        outputReference
    );
  }
}