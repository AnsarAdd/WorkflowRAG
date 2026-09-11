package com.ansaradd.workflowragsrc.workflow.persistence.postgres;

import com.ansaradd.workflowragsrc.workflow.exception.InvalidJobStepStateException;
import com.ansaradd.workflowragsrc.workflow.exception.JobStepNotFoundException;
import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import com.ansaradd.workflowragsrc.workflow.model.JobStepStatus;
import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresJobStepRepository implements JobStepRepository {

  private final JdbcClient jdbcClient;

  public PostgresJobStepRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public List<JobStep> createPendingSteps(
      UUID jobId,
      List<String> stages
  ) {
    List<JobStep> createdSteps =
        new ArrayList<>(stages.size());

    for (int stepOrder = 0;
        stepOrder < stages.size();
        stepOrder++) {

      String stage = stages.get(stepOrder);
      UUID stepId = UUID.randomUUID();

      int inserted = jdbcClient.sql("""
              INSERT INTO job_step (
                  id,
                  job_id,
                  step_order,
                  stage,
                  status,
                  attempt
              )
              VALUES (
                  :id,
                  :jobId,
                  :stepOrder,
                  :stage,
                  'PENDING',
                  0
              )
              """)
          .param("id", stepId)
          .param("jobId", jobId)
          .param("stepOrder", stepOrder)
          .param("stage", stage)
          .update();

      if (inserted != 1) {
        throw new IllegalStateException(
            "Failed to create job step: " + stage
        );
      }

      createdSteps.add(
          new JobStep(
              stepId,
              jobId,
              stepOrder,
              stage,
              JobStepStatus.PENDING,
              0,
              null,
              null,
              null,
              null,
              null
          )
      );
    }

    return List.copyOf(createdSteps);
  }

  @Override
  public List<UUID> findRunningIds() {
    return jdbcClient.sql("""
            SELECT id
            FROM job_step
            WHERE status = 'RUNNING'
            ORDER BY job_id, step_order
            """)
        .query(UUID.class)
        .list();
  }

  @Override
  public List<JobStep> findByJobId(UUID jobId) {
    return jdbcClient.sql("""
            SELECT
                id,
                job_id,
                step_order,
                stage,
                status,
                attempt,
                fingerprint,
                output_reference,
                started_at,
                completed_at,
                error
            FROM job_step
            WHERE job_id = :jobId
            ORDER BY step_order
            """)
        .param("jobId", jobId)
        .query(this::mapJobStep)
        .list();
  }

  @Override
  public Optional<JobStep> findById(UUID stepId) {
    return jdbcClient.sql("""
            SELECT
                id,
                job_id,
                step_order,
                stage,
                status,
                attempt,
                fingerprint,
                output_reference,
                started_at,
                completed_at,
                error
            FROM job_step
            WHERE id = :stepId
            """)
        .param("stepId", stepId)
        .query(this::mapJobStep)
        .optional();
  }

  @Override
  public JobStep start(UUID stepId) {
    Optional<JobStep> started =
        jdbcClient.sql("""
                UPDATE job_step
                SET
                    status = 'RUNNING',
                    attempt = attempt + 1,
                    started_at = CURRENT_TIMESTAMP,
                    completed_at = NULL,
                    fingerprint = NULL,
                    error = NULL
                WHERE id = :stepId
                  AND status IN ('PENDING', 'FAILED')
                RETURNING
                    id,
                    job_id,
                    step_order,
                    stage,
                    status,
                    attempt,
                    fingerprint,
                    started_at,
                    completed_at,
                    error
                """)
            .param("stepId", stepId)
            .query(this::mapJobStep)
            .optional();

    if (started.isPresent()) {
      return started.get();
    }

    JobStep existing =
        findById(stepId)
            .orElseThrow(
                () -> new JobStepNotFoundException(stepId)
            );

    throw new InvalidJobStepStateException(
        stepId,
        existing.status(),
        "start"
    );
  }

  @Override
  public void markSucceeded(
      UUID stepId,
      String fingerprint,
      String result
  ) {
    int updated =
        jdbcClient.sql("""
                UPDATE job_step
                SET
                    status = 'SUCCEEDED',
                    fingerprint = :fingerprint,
                    completed_at = CURRENT_TIMESTAMP,
                    error = NULL
                WHERE id = :stepId
                  AND status = 'RUNNING'
                """)
            .param("stepId", stepId)
            .param("fingerprint", fingerprint)
            .update();

    if (updated == 1) {
      return;
    }

    JobStep existing =
        findRequired(stepId);

    if (existing.status()
        == JobStepStatus.SUCCEEDED) {
      return;
    }

    throw new InvalidJobStepStateException(
        stepId,
        existing.status(),
        "succeed"
    );
  }

  @Override
  public void markFailed(
      UUID stepId,
      String error
  ) {
    int updated =
        jdbcClient.sql("""
                UPDATE job_step
                SET
                    status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP,
                    error = :error
                WHERE id = :stepId
                  AND status = 'RUNNING'
                """)
            .param("stepId", stepId)
            .param("error", error)
            .update();

    if (updated == 1) {
      return;
    }

    JobStep existing =
        findRequired(stepId);

    if (existing.status()
        == JobStepStatus.FAILED) {
      return;
    }

    throw new InvalidJobStepStateException(
        stepId,
        existing.status(),
        "fail"
    );
  }

  @Override
  public void markSkipped(
      UUID stepId,
      String fingerprint,
      String reason
  ) {
    int updated =
        jdbcClient.sql("""
                UPDATE job_step
                SET
                    status = 'SKIPPED',
                    fingerprint = :fingerprint,
                    completed_at = CURRENT_TIMESTAMP,
                    error = :reason
                WHERE id = :stepId
                  AND status IN ('PENDING', 'FAILED')
                """)
            .param("stepId", stepId)
            .param("fingerprint", fingerprint)
            .param("reason", reason)
            .update();

    if (updated == 1) {
      return;
    }

    JobStep existing =
        findRequired(stepId);

    if (existing.status()
        == JobStepStatus.SKIPPED) {
      return;
    }

    throw new InvalidJobStepStateException(
        stepId,
        existing.status(),
        "skip"
    );
  }

  private JobStep mapJobStep(
      ResultSet rs,
      int rowNum
  ) throws SQLException {
    Timestamp startedAt =
        rs.getTimestamp("started_at");

    Timestamp completedAt =
        rs.getTimestamp("completed_at");

    return new JobStep(
        rs.getObject("id", UUID.class),
        rs.getObject("job_id", UUID.class),
        rs.getInt("step_order"),
        rs.getString("stage"),
        JobStepStatus.valueOf(
            rs.getString("status")
        ),
        rs.getInt("attempt"),
        rs.getString("fingerprint"),
        rs.getString("output_reference"),
        startedAt == null
            ? null
            : startedAt.toInstant(),
        completedAt == null
            ? null
            : completedAt.toInstant(),
        rs.getString("error")
    );
  }

  private JobStep findRequired(UUID stepId) {
    return findById(stepId)
        .orElseThrow(
            () -> new JobStepNotFoundException(stepId)
        );
  }
}