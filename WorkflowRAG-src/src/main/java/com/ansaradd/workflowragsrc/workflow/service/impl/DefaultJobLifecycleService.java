package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultJobLifecycleService
    implements JobLifecycleService {

  private final JobRepository jobRepository;
  private final JobStepRepository jobStepRepository;

  public DefaultJobLifecycleService(
      JobRepository jobRepository,
      JobStepRepository jobStepRepository
  ) {
    this.jobRepository = jobRepository;
    this.jobStepRepository = jobStepRepository;
  }

  @Override
  @Transactional
  public Job create(
      JobType type,
      PipelineTemplate pipeline,
      String sourceId,
      UUID documentId,
      UUID documentVersionId
  ) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(pipeline, "pipeline must not be null");
    Objects.requireNonNull(sourceId, "sourceId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );

    if (sourceId.isBlank()) {
      throw new IllegalArgumentException(
          "sourceId must not be blank"
      );
    }

    Job job = jobRepository.create(
        type,
        pipeline.id(),
        sourceId,
        documentId,
        documentVersionId
    );

    jobStepRepository.createPendingSteps(
        job.id(),
        pipeline.stages()
    );

    return job;
  }

  @Override
  public Job start(UUID jobId) {
    return jobRepository.start(jobId);
  }

  @Override
  public void setCurrentStage(
      UUID jobId,
      String stage
  ) {
    jobRepository.updateCurrentStage(
        jobId,
        stage
    );
  }

  @Override
  public void complete(UUID jobId) {
    jobRepository.markCompleted(jobId);
  }

  @Override
  public void fail(
      UUID jobId,
      String error
  ) {
    jobRepository.markFailed(
        jobId,
        error
    );
  }
}