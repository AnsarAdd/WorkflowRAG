package com.ansaradd.workflowragsrc.document.persistence.postgres;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragsrc.document.exception.DocumentNotFoundException;
import com.ansaradd.workflowragsrc.document.exception.DocumentVersionBuildInProgressException;
import com.ansaradd.workflowragsrc.document.exception.DocumentVersionNotFoundException;
import com.ansaradd.workflowragsrc.document.exception.InvalidDocumentVersionStateException;
import com.ansaradd.workflowragsrc.document.model.ActiveDocumentVersion;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionContent;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionStatus;
import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDocumentVersionRepository
    implements DocumentVersionRepository {

  private final JdbcClient jdbcClient;

  public PostgresDocumentVersionRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public Optional<ActiveDocumentVersion> findActive(UUID documentId) {
    return jdbcClient.sql("""
            SELECT
                id,
                document_id,
                version_number,
                content_hash
            FROM document_version
            WHERE document_id = :documentId
              AND status = 'ACTIVE'
            """)
        .param("documentId", documentId)
        .query((rs, rowNum) ->
            new ActiveDocumentVersion(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getLong("version_number"),
                rs.getString("content_hash")
            )
        )
        .optional();
  }

  @Override
  public DocumentVersionContent getContent(UUID versionId) {
    return jdbcClient.sql("""
          SELECT
              id,
              document_id,
              format,
              content_hash,
              content,
              status
          FROM document_version
          WHERE id = :versionId
          """)
        .param("versionId", versionId)
        .query((rs, rowNum) ->
            new DocumentVersionContent(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                DocumentFormat.valueOf(
                    rs.getString("format")
                ),
                rs.getString("content_hash"),
                rs.getBytes("content"),
                DocumentVersionStatus.valueOf(
                    rs.getString("status")
                )
            )
        )
        .optional()
        .orElseThrow(
            () -> new DocumentVersionNotFoundException(versionId)
        );
  }

  @Override
  @Transactional
  public StoredDocumentVersion createNextBuildingVersion(
      UUID documentId,
      DocumentFormat format,
      String contentHash,
      byte[] content
  ) {
    lockDocument(documentId);

    if (hasBuildingVersion(documentId)) {
      throw new DocumentVersionBuildInProgressException(documentId);
    }

    long nextVersionNumber = getNextVersionNumber(documentId);
    UUID versionId = UUID.randomUUID();

    int inserted = jdbcClient.sql("""
            INSERT INTO document_version (
                id,
                document_id,
                version_number,
                format,
                content_hash,
                content,
                status
            )
            VALUES (
                :id,
                :documentId,
                :versionNumber,
                :format,
                :contentHash,
                :content,
                'BUILDING'
            )
            """)
        .param("id", versionId)
        .param("documentId", documentId)
        .param("versionNumber", nextVersionNumber)
        .param("format", format.name())
        .param("contentHash", contentHash)
        .param("content", content)
        .update();

    if (inserted != 1) {
      throw new IllegalStateException(
          "Failed to create document version for document: " + documentId
      );
    }

    return new StoredDocumentVersion(
        versionId,
        documentId,
        nextVersionNumber,
        DocumentVersionStatus.BUILDING
    );
  }

  @Override
  @Transactional
  public void activate(UUID versionId) {
    UUID documentId = getRequiredDocumentId(versionId);

    lockDocument(documentId);

    DocumentVersionStatus status = getRequiredStatus(versionId);

    if (status == DocumentVersionStatus.ACTIVE) {
      return;
    }

    if (status != DocumentVersionStatus.BUILDING) {
      throw new InvalidDocumentVersionStateException(
          versionId,
          status,
          "activate"
      );
    }

    jdbcClient.sql("""
            UPDATE document_version
            SET status = 'INACTIVE'
            WHERE document_id = :documentId
              AND status = 'ACTIVE'
            """)
        .param("documentId", documentId)
        .update();

    int activated = jdbcClient.sql("""
            UPDATE document_version
            SET
                status = 'ACTIVE',
                activated_at = CURRENT_TIMESTAMP
            WHERE id = :versionId
              AND status = 'BUILDING'
            """)
        .param("versionId", versionId)
        .update();

    if (activated != 1) {
      throw new IllegalStateException(
          "Failed to activate document version: " + versionId
      );
    }
  }

  @Override
  @Transactional
  public void markFailed(UUID versionId) {
    UUID documentId = getRequiredDocumentId(versionId);

    lockDocument(documentId);

    DocumentVersionStatus status = getRequiredStatus(versionId);

    if (status == DocumentVersionStatus.FAILED) {
      return;
    }

    if (status != DocumentVersionStatus.BUILDING) {
      throw new InvalidDocumentVersionStateException(
          versionId,
          status,
          "mark as failed"
      );
    }

    int updated = jdbcClient.sql("""
            UPDATE document_version
            SET status = 'FAILED'
            WHERE id = :versionId
              AND status = 'BUILDING'
            """)
        .param("versionId", versionId)
        .update();

    if (updated != 1) {
      throw new IllegalStateException(
          "Failed to mark document version as FAILED: " + versionId
      );
    }
  }

  @Override
  @Transactional
  public void resume(UUID versionId) {
    UUID documentId = getRequiredDocumentId(versionId);
    lockDocument(documentId);
    DocumentVersionStatus status = getRequiredStatus(versionId);
    // ACTIVE is valid when activation committed just before process shutdown.
    if (status == DocumentVersionStatus.BUILDING || status == DocumentVersionStatus.ACTIVE) {
      return;
    }
    boolean newerVersion = jdbcClient.sql("""
        SELECT EXISTS (
          SELECT 1 FROM document_version newer
          JOIN document_version current ON current.id = :versionId
          WHERE newer.document_id = current.document_id
            AND newer.version_number > current.version_number
        )
        """).param("versionId", versionId).query(Boolean.class).single();
    if (status != DocumentVersionStatus.FAILED || newerVersion) {
      throw new InvalidDocumentVersionStateException(versionId, status, "resume superseded version");
    }
    if (hasBuildingVersion(documentId)) {
      throw new DocumentVersionBuildInProgressException(documentId);
    }
    jdbcClient.sql("UPDATE document_version SET status = 'BUILDING' WHERE id = :id")
        .param("id", versionId).update();
  }

  private void lockDocument(UUID documentId) {
    Optional<UUID> document = jdbcClient.sql("""
            SELECT id
            FROM document
            WHERE id = :documentId
            FOR UPDATE
            """)
        .param("documentId", documentId)
        .query(UUID.class)
        .optional();

    if (document.isEmpty()) {
      throw new DocumentNotFoundException(documentId);
    }
  }

  private boolean hasBuildingVersion(UUID documentId) {
    return jdbcClient.sql("""
            SELECT EXISTS (
                SELECT 1
                FROM document_version
                WHERE document_id = :documentId
                  AND status = 'BUILDING'
            )
            """)
        .param("documentId", documentId)
        .query(Boolean.class)
        .single();
  }

  private long getNextVersionNumber(UUID documentId) {
    return jdbcClient.sql("""
            SELECT COALESCE(MAX(version_number), 0) + 1
            FROM document_version
            WHERE document_id = :documentId
            """)
        .param("documentId", documentId)
        .query(Long.class)
        .single();
  }

  private UUID getRequiredDocumentId(UUID versionId) {
    return jdbcClient.sql("""
            SELECT document_id
            FROM document_version
            WHERE id = :versionId
            """)
        .param("versionId", versionId)
        .query(UUID.class)
        .optional()
        .orElseThrow(
            () -> new DocumentVersionNotFoundException(versionId)
        );
  }

  private DocumentVersionStatus getRequiredStatus(UUID versionId) {
    String status = jdbcClient.sql("""
            SELECT status
            FROM document_version
            WHERE id = :versionId
            """)
        .param("versionId", versionId)
        .query(String.class)
        .optional()
        .orElseThrow(
            () -> new DocumentVersionNotFoundException(versionId)
        );

    return DocumentVersionStatus.valueOf(status);
  }
}