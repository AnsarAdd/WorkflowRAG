package com.ansaradd.workflowragsrc.retrieval.persistence.postgres;

import com.ansaradd.workflowragsrc.chunk.exception.DocumentChunksNotFoundException;
import com.ansaradd.workflowragsrc.retrieval.exception.IncompleteEmbeddingIndexException;
import com.ansaradd.workflowragsrc.retrieval.repository.RetrievalIndexRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresRetrievalIndexRepository
    implements RetrievalIndexRepository {

  private final JdbcClient jdbcClient;

  public PostgresRetrievalIndexRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public void replaceForVersion(
      UUID documentVersionId
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );

    long chunkCount =
        countChunks(documentVersionId);

    if (chunkCount == 0) {
      throw new DocumentChunksNotFoundException(
          documentVersionId
      );
    }

    long embeddingCount =
        countEmbeddings(documentVersionId);

    if (chunkCount != embeddingCount) {
      throw new IncompleteEmbeddingIndexException(
          documentVersionId,
          chunkCount,
          embeddingCount
      );
    }

    deleteForVersion(
        documentVersionId
    );

    int inserted =
        insertForVersion(
            documentVersionId
        );

    if (inserted != chunkCount) {
      throw new IllegalStateException(
          "Failed to build retrieval index for document version "
              + documentVersionId
              + ": expected="
              + chunkCount
              + ", inserted="
              + inserted
      );
    }
  }

  private long countChunks(
      UUID documentVersionId
  ) {
    return jdbcClient.sql("""
            SELECT COUNT(*)
            FROM document_chunk c
            JOIN document_section s
              ON s.id = c.section_id
            WHERE s.document_version_id = :documentVersionId
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .query(Long.class)
        .single();
  }

  private long countEmbeddings(
      UUID documentVersionId
  ) {
    return jdbcClient.sql("""
            SELECT COUNT(*)
            FROM chunk_embedding e
            JOIN document_chunk c
              ON c.id = e.chunk_id
            JOIN document_section s
              ON s.id = c.section_id
            WHERE s.document_version_id = :documentVersionId
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .query(Long.class)
        .single();
  }

  private void deleteForVersion(
      UUID documentVersionId
  ) {
    jdbcClient.sql("""
            DELETE FROM retrieval_chunk r
            USING document_chunk c,
                  document_section s
            WHERE r.chunk_id = c.id
              AND c.section_id = s.id
              AND s.document_version_id = :documentVersionId
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .update();
  }

  private int insertForVersion(
      UUID documentVersionId
  ) {
    return jdbcClient.sql("""
            INSERT INTO retrieval_chunk (
                chunk_id,
                search_vector
            )
            SELECT
                c.id,
                setweight(
                    to_tsvector(
                        'simple'::regconfig,
                        COALESCE(s.title, '')
                    ),
                    'A'
                )
                ||
                setweight(
                    to_tsvector(
                        'simple'::regconfig,
                        c.content
                    ),
                    'B'
                )
            FROM document_chunk c
            JOIN document_section s
              ON s.id = c.section_id
            JOIN chunk_embedding e
              ON e.chunk_id = c.id
            WHERE s.document_version_id = :documentVersionId
            ON CONFLICT (chunk_id)
            DO NOTHING
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .update();
  }
}