package com.ansaradd.workflowragsrc.workflow.persistence.postgres;

import com.ansaradd.workflowragsrc.workflow.exception.InvalidJobStateException;
import com.ansaradd.workflowragsrc.workflow.exception.JobNotFoundException;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobStatus;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresJobRepository implements JobRepository {

  private final JdbcClient jdbcClient;

  public PostgresJobRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public Job create(
      JobType type,
      String pipelineId,
      String sourceId,
      UUID documentId,
      UUID documentVersionId
  ) {
    UUID jobId = UUID.randomUUID();

    return jdbcClient.sql("""
          INSERT INTO job (
              id,
              type,
              pipeline_id,
              source_id,
              document_id,
              document_version_id,
              status
          )
          VALUES (
              :id,
              :type,
              :pipelineId,
              :sourceId,
              :documentId,
              :documentVersionId,
              'PENDING'
          )
          RETURNING
              id,
              type,
              pipeline_id,
              source_id,
              document_id,
              document_version_id,
              status,
              current_stage,
              created_at,
              started_at,
              completed_at,
              error
          """)
        .param("id", jobId)
        .param("type", type.name())
        .param("pipelineId", pipelineId)
        .param("sourceId", sourceId)
        .param("documentId", documentId)
        .param("documentVersionId", documentVersionId)
        .query(this::mapJob)
        .single();
  }

  @Override
  public List<UUID> findRecoverableIds() {
    return jdbcClient.sql("""
          SELECT j.id
          FROM job j
          JOIN document_version v ON v.id = j.document_version_id
          WHERE j.status IN ('PENDING', 'RUNNING')
             OR (j.status = 'FAILED' AND v.status = 'BUILDING')
          ORDER BY j.created_at
          """)
        .query(UUID.class)
        .list();
  }

  private Job mapJob(
      ResultSet rs,
      int rowNum
  ) throws SQLException {
    Timestamp startedAt =
        rs.getTimestamp("started_at");

    Timestamp completedAt =
        rs.getTimestamp("completed_at");

    return new Job(
        rs.getObject("id", UUID.class),
        JobType.valueOf(rs.getString("type")),
        rs.getString("pipeline_id"),
        rs.getString("source_id"),
        rs.getObject("document_id", UUID.class),
        rs.getObject("document_version_id", UUID.class),
        JobStatus.valueOf(rs.getString("status")),
        rs.getString("current_stage"),
        rs.getTimestamp("created_at").toInstant(),
        startedAt == null
            ? null
            : startedAt.toInstant(),
        completedAt == null
            ? null
            : completedAt.toInstant(),
        rs.getString("error")
    );
  }

  @Override
  public Optional<Job> findById(UUID jobId) {
    return jdbcClient.sql("""
          SELECT
              id,
              type,
              pipeline_id,
              source_id,
              document_id,
              document_version_id,
              status,
              current_stage,
              created_at,
              started_at,
              completed_at,
              error
          FROM job
          WHERE id = :jobId
          """)
        .param("jobId", jobId)
        .query(this::mapJob)
        .optional();
  }

  @Override
  public Job start(UUID jobId) {
    Optional<Job> started =
        jdbcClient.sql("""
              UPDATE job
              SET
                  status = 'RUNNING',
                  started_at = COALESCE(
                      started_at,
                      CURRENT_TIMESTAMP
                  ),
                  completed_at = NULL,
                  error = NULL
              WHERE id = :jobId
                AND status IN ('PENDING', 'FAILED')
              RETURNING
                  id,
                  type,
                  pipeline_id,
                  source_id,
                  document_id,
                  document_version_id,
                  status,
                  current_stage,
                  created_at,
                  started_at,
                  completed_at,
                  error
              """)
            .param(
                "jobId",
                jobId
            )
            .query(this::mapJob)
            .optional();

    if (started.isPresent()) {
      return started.get();
    }

    Job existing =
        findById(jobId)
            .orElseThrow(
                () -> new JobNotFoundException(
                    jobId
                )
            );

    throw new InvalidJobStateException(
        jobId,
        existing.status(),
        "start"
    );
  }

  @Override
  public void updateCurrentStage(
      UUID jobId,
      String stage
  ) {
    int updated = jdbcClient.sql("""
          UPDATE job
          SET current_stage = :stage
          WHERE id = :jobId
            AND status = 'RUNNING'
          """)
        .param("jobId", jobId)
        .param("stage", stage)
        .update();

    if (updated == 1) {
      return;
    }

    Job existing = findById(jobId)
        .orElseThrow(() -> new JobNotFoundException(jobId));

    throw new InvalidJobStateException(
        jobId,
        existing.status(),
        "update current stage"
    );
  }

  @Override
  public void markCompleted(UUID jobId) {
    int updated = jdbcClient.sql("""
          UPDATE job
          SET
              status = 'COMPLETED',
              current_stage = NULL,
              completed_at = CURRENT_TIMESTAMP,
              error = NULL
          WHERE id = :jobId
            AND status = 'RUNNING'
          """)
        .param("jobId", jobId)
        .update();

    if (updated == 1) {
      return;
    }

    Job existing = findById(jobId)
        .orElseThrow(() -> new JobNotFoundException(jobId));

    if (existing.status() == JobStatus.COMPLETED) {
      return;
    }

    throw new InvalidJobStateException(
        jobId,
        existing.status(),
        "complete"
    );
  }

  @Override
  public void markFailed(
      UUID jobId,
      String error
  ) {
    int updated = jdbcClient.sql("""
          UPDATE job
          SET
              status = 'FAILED',
              completed_at = CURRENT_TIMESTAMP,
              error = :error
          WHERE id = :jobId
            AND status IN ('PENDING', 'RUNNING')
          """)
        .param("jobId", jobId)
        .param("error", error)
        .update();

    if (updated == 1) {
      return;
    }

    Job existing = findById(jobId)
        .orElseThrow(() -> new JobNotFoundException(jobId));

    if (existing.status() == JobStatus.FAILED) {
      return;
    }

    throw new InvalidJobStateException(
        jobId,
        existing.status(),
        "mark as failed"
    );
  }
}