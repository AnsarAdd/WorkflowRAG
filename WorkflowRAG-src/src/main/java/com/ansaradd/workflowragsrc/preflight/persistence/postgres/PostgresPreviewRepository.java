package com.ansaradd.workflowragsrc.preflight.persistence.postgres;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragsrc.preflight.model.PreviewSnapshot;
import com.ansaradd.workflowragsrc.preflight.repository.PreviewRepository;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PostgresPreviewRepository implements PreviewRepository {
  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public PostgresPreviewRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(PreviewSnapshot snapshot) {
    var report = snapshot.report();
    jdbcClient.sql("""
        INSERT INTO ingestion_preview (id, format, content, force_reindex, report, expires_at)
        VALUES (:id, :format, :content, :force, CAST(:report AS jsonb), :expiry)
        """)
        .param("id", report.id())
        .param("format", snapshot.document().format().name())
        .param("content", snapshot.document().content())
        .param("force", snapshot.forceReindex())
        .param("report", objectMapper.writeValueAsString(report))
        .param("expiry", Timestamp.from(report.expiresAt()))
        .update();
  }

  @Override
  public Optional<PreviewSnapshot> find(UUID id, boolean lock) {
    String sql = "SELECT * FROM ingestion_preview WHERE id=:id" + (lock ? " FOR UPDATE" : "");
    return jdbcClient.sql(sql).param("id", id).query((rs, row) -> {
      var report = objectMapper.readValue(rs.getString("report"), IngestionPreviewResponse.class);
      UUID job = rs.getObject("job_id", UUID.class);
      // Expiry is a column so cleanup and expiry checks use the same value.
      report = new IngestionPreviewResponse(report.id(), report.sourceId(), report.externalDocumentId(),
          report.contentHash(), report.baselineVersionId(), report.status(), report.canConfirm(),
          rs.getTimestamp("expires_at").toInstant(), report.changes(), report.similarity(), job);
      if (job != null) {
        report = report.withState("CONFIRMED", false, job);
      }
      byte[] content = rs.getBytes("content");
      var document = new PreparedDocument(new DocumentKey(report.sourceId(), report.externalDocumentId()),
          DocumentFormat.valueOf(rs.getString("format")), content == null ? new byte[0] : content,
          report.contentHash(), Map.of());
      return new PreviewSnapshot(document, rs.getBoolean("force_reindex"), report);
    }).optional();
  }

  @Override
  public void confirm(UUID id, UUID jobId) {
    jdbcClient.sql("UPDATE ingestion_preview SET job_id=:job, content=NULL WHERE id=:id")
        .param("job", jobId).param("id", id).update();
  }

  @Override
  public void lockDocument(UUID documentId) {
    jdbcClient.sql("SELECT id FROM document WHERE id=:id FOR UPDATE")
        .param("id", documentId).query(UUID.class).single();
  }

  @Override
  public void deleteExpired() {
    jdbcClient.sql("DELETE FROM ingestion_preview WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day'")
        .update();
  }
}
