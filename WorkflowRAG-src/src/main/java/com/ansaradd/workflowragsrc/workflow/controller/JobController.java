package com.ansaradd.workflowragsrc.workflow.controller;

import com.ansaradd.workflowragsrc.workflow.exception.JobNotFoundException;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
  private final JobRepository jobs;
  private final JobStepRepository steps;
  private final WorkflowExecutor executor;

  public JobController(JobRepository jobs, JobStepRepository steps, WorkflowExecutor executor) {
    this.jobs = jobs;
    this.steps = steps;
    this.executor = executor;
  }

  @GetMapping("/{id}")
  public JobResponse get(@PathVariable UUID id) {
    Job job = jobs.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    return new JobResponse(job, steps.findByJobId(id));
  }

  @PostMapping("/{id}/retry")
  public JobResponse retry(@PathVariable UUID id) {
    executor.execute(id);
    return get(id);
  }

  public record JobResponse(Job job, List<JobStep> steps) {}
}