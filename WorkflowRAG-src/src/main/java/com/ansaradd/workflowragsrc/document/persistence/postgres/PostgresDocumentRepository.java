package com.ansaradd.workflowragsrc.document.persistence.postgres;

import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.document.repository.DocumentRepository;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDocumentRepository implements DocumentRepository {

  private final JdbcClient jdbcClient;

  public PostgresDocumentRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public Optional<StoredDocument> findByKey(DocumentKey documentKey) {
    return jdbcClient.sql("""
            SELECT
                id,
                source_id,
                external_document_id
            FROM document
            WHERE source_id = :sourceId
              AND external_document_id = :externalDocumentId
            """)
        .param("sourceId", documentKey.sourceId())
        .param(
            "externalDocumentId",
            documentKey.externalDocumentId()
        )
        .query((rs, rowNum) ->
            new StoredDocument(
                rs.getObject("id", UUID.class),
                new DocumentKey(
                    rs.getString("source_id"),
                    rs.getString("external_document_id")
                )
            )
        )
        .optional();
  }

  @Override
  @Transactional
  public StoredDocument getOrCreate(DocumentKey documentKey) {
    UUID documentId = UUID.randomUUID();

    jdbcClient.sql("""
            INSERT INTO document (
                id,
                source_id,
                external_document_id
            )
            VALUES (
                :id,
                :sourceId,
                :externalDocumentId
            )
            ON CONFLICT (
                source_id,
                external_document_id
            )
            DO NOTHING
            """)
        .param("id", documentId)
        .param("sourceId", documentKey.sourceId())
        .param(
            "externalDocumentId",
            documentKey.externalDocumentId()
        )
        .update();

    return findByKey(documentKey)
        .orElseThrow(() ->
            new IllegalStateException(
                "Document was not found after getOrCreate: "
                    + documentKey
            )
        );
  }
}