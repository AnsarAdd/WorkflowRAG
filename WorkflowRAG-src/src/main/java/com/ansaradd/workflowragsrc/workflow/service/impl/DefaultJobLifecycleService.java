package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
import java.util.Objects;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionStatus;
import com.ansaradd.workflowragsrc.workflow.exception.JobNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultJobLifecycleService
    implements JobLifecycleService {

  private final DocumentVersionRepository versionRepository;
  private final JobRepository jobRepository;
  private final JobStepRepository jobStepRepository;

  public DefaultJobLifecycleService(
      JobRepository jobRepository,
      JobStepRepository jobStepRepository,
      DocumentVersionRepository versionRepository
  ) {
    this.versionRepository = versionRepository;
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
  @Transactional
  public Job start(UUID jobId) {
    Job job = jobRepository.start(jobId);
    versionRepository.resume(job.documentVersionId());
    return job;
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
  @Transactional
  public void fail(
      UUID jobId,
      String error
  ) {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    jobRepository.markFailed(jobId, error);
    if (versionRepository.getContent(job.documentVersionId()).status() == DocumentVersionStatus.BUILDING) {
      versionRepository.markFailed(job.documentVersionId());
    }
  }
}