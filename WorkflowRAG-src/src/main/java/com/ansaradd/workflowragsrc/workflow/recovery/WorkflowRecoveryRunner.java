package com.ansaradd.workflowragsrc.workflow.recovery;

import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
import com.ansaradd.workflowragsrc.workflow.service.JobStepLifecycleService;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowRecoveryRunner
    implements ApplicationRunner {

  private static final String RECOVERY_ERROR =
      "Execution was interrupted by application shutdown";

  private final JobRepository jobRepository;
  private final JobStepRepository jobStepRepository;
  private final JobLifecycleService jobLifecycleService;
  private final JobStepLifecycleService jobStepLifecycleService;

  public WorkflowRecoveryRunner(
      JobRepository jobRepository,
      JobStepRepository jobStepRepository,
      JobLifecycleService jobLifecycleService,
      JobStepLifecycleService jobStepLifecycleService
  ) {
    this.jobRepository = jobRepository;
    this.jobStepRepository = jobStepRepository;
    this.jobLifecycleService = jobLifecycleService;
    this.jobStepLifecycleService =
        jobStepLifecycleService;
  }

  @Override
  @Transactional
  public void run(
      ApplicationArguments args
  ) {
    recoverRunningSteps();
    recoverRunningJobs();
  }

  private void recoverRunningSteps() {
    List<UUID> stepIds =
        jobStepRepository.findRunningIds();

    for (UUID stepId : stepIds) {
      jobStepLifecycleService.fail(
          stepId,
          RECOVERY_ERROR
      );
    }
  }

  private void recoverRunningJobs() {
    List<UUID> jobIds =
        jobRepository.findRunningIds();

    for (UUID jobId : jobIds) {
      jobLifecycleService.fail(
          jobId,
          RECOVERY_ERROR
      );
    }
  }
}