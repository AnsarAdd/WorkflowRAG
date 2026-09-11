package com.ansaradd.workflowragsrc.workflow.service.impl;

import com.ansaradd.workflowragsrc.workflow.exception.WorkflowExecutionException;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import com.ansaradd.workflowragsrc.workflow.model.JobStepStatus;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
import com.ansaradd.workflowragsrc.workflow.service.JobStepLifecycleService;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowStageResolver;
import com.ansaradd.workflowragsrc.workflow.stage.IdempotentWorkflowStage;
import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultWorkflowExecutor
    implements WorkflowExecutor {

  private final JobLifecycleService jobLifecycleService;
  private final JobStepLifecycleService jobStepLifecycleService;
  private final JobStepRepository jobStepRepository;
  private final WorkflowStageResolver workflowStageResolver;

  public DefaultWorkflowExecutor(
      JobLifecycleService jobLifecycleService,
      JobStepLifecycleService jobStepLifecycleService,
      JobStepRepository jobStepRepository,
      WorkflowStageResolver workflowStageResolver
  ) {
    this.jobLifecycleService = jobLifecycleService;
    this.jobStepLifecycleService = jobStepLifecycleService;
    this.jobStepRepository = jobStepRepository;
    this.workflowStageResolver = workflowStageResolver;
  }

  private static final Logger log =
      LoggerFactory.getLogger(
          DefaultWorkflowExecutor.class
      );

  @Override
  public void execute(UUID jobId) {
    Job job =
        jobLifecycleService.start(
            jobId
        );

    log.info(
        "Workflow job started: jobId={}, pipelineId={}, documentVersionId={}",
        job.id(),
        job.pipelineId(),
        job.documentVersionId()
    );

    List<JobStep> steps =
        jobStepRepository.findByJobId(
            jobId
        );

    for (JobStep step : steps) {
      if (isAlreadyCompleted(step)) {
        log.debug(
            "Workflow step already completed: jobId={}, stepId={}, stage={}, status={}",
            job.id(),
            step.id(),
            step.stage(),
            step.status()
        );

        continue;
      }

      executeStep(
          job,
          step
      );
    }

    jobLifecycleService.complete(
        job.id()
    );

    log.info(
        "Workflow job completed: jobId={}, documentVersionId={}",
        job.id(),
        job.documentVersionId()
    );
  }

  private void executeStep(
      Job job,
      JobStep step
  ) {
    JobStep runningStep = null;

    try {
      jobLifecycleService.setCurrentStage(
          job.id(),
          step.stage()
      );

      WorkflowStage workflowStage =
          workflowStageResolver.resolve(
              step.stage()
          );

      String fingerprint = null;

      if (workflowStage
          instanceof IdempotentWorkflowStage idempotentStage) {

        fingerprint =
            idempotentStage.fingerprint(
                job
            );

        if (idempotentStage.canReuse(
            job,
            fingerprint
        )) {
          jobStepLifecycleService.skip(
              step.id(),
              fingerprint,
              null
          );

          return;
        }
      }

      runningStep =
          jobStepLifecycleService.start(
              step.id()
          );

      workflowStage.execute(job);

      jobStepLifecycleService.succeed(
          runningStep.id(),
          fingerprint,
          null
      );

    } catch (Exception exception) {
      handleFailure(
          job,
          step,
          runningStep,
          exception
      );
    }
  }

  private void handleFailure(
      Job job,
      JobStep step,
      JobStep runningStep,
      Exception exception
  ) {
    JobStep failedStep =
        runningStep;

    /*
     * Resolver, fingerprint calculation, canReuse or
     * setCurrentStage may fail before the step reaches RUNNING.
     *
     * For a normal PENDING/FAILED step we still record
     * an actual failed attempt.
     */
    if (failedStep == null) {
      try {
        failedStep =
            jobStepLifecycleService.start(
                step.id()
            );
      } catch (Exception transitionException) {
        exception.addSuppressed(
            transitionException
        );
      }
    }

    String error =
        errorMessage(exception);

    if (failedStep != null) {
      try {
        jobStepLifecycleService.fail(
            failedStep.id(),
            error
        );
      } catch (Exception transitionException) {
        exception.addSuppressed(
            transitionException
        );
      }
    }

    try {
      jobLifecycleService.fail(
          job.id(),
          error
      );
    } catch (Exception transitionException) {
      exception.addSuppressed(
          transitionException
      );
    }

    throw new WorkflowExecutionException(
        job.id(),
        step.stage(),
        exception
    );
  }

  private boolean isAlreadyCompleted(
      JobStep step
  ) {
    return step.status()
        == JobStepStatus.SUCCEEDED
        || step.status()
        == JobStepStatus.SKIPPED;
  }

  private String errorMessage(
      Exception exception
  ) {
    String message =
        exception.getMessage();

    if (message != null
        && !message.isBlank()) {
      return message;
    }

    return exception
        .getClass()
        .getName();
  }
}