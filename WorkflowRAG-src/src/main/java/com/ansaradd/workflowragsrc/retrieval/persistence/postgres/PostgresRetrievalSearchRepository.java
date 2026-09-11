package com.ansaradd.workflowragsrc.retrieval.persistence.postgres;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.repository.RetrievalSearchRepository;
import com.pgvector.PGvector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresRetrievalSearchRepository
    implements RetrievalSearchRepository {

  private final JdbcClient jdbcClient;

  public PostgresRetrievalSearchRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public List<RetrievalHit> search(
      float[] queryEmbedding,
      String provider,
      String model,
      String modelRevision,
      int dimensions,
      double minScore,
      String sourceId,
      int limit) {
    if (minScore < -1.0 || minScore > 1.0) {
      throw new IllegalArgumentException(
          "minScore must be between -1 and 1"
      );
    }
    Objects.requireNonNull(
        queryEmbedding,
        "queryEmbedding must not be null"
    );
    Objects.requireNonNull(
        provider,
        "provider must not be null"
    );
    Objects.requireNonNull(
        model,
        "model must not be null"
    );

    if (provider.isBlank()) {
      throw new IllegalArgumentException(
          "provider must not be blank"
      );
    }

    if (model.isBlank()) {
      throw new IllegalArgumentException(
          "model must not be blank"
      );
    }

    if (dimensions <= 0) {
      throw new IllegalArgumentException(
          "dimensions must be positive"
      );
    }

    if (queryEmbedding.length != dimensions) {
      throw new IllegalArgumentException(
          "Query embedding dimensions mismatch: expected="
              + dimensions
              + ", actual="
              + queryEmbedding.length
      );
    }

    if (limit <= 0) {
      throw new IllegalArgumentException(
          "limit must be positive"
      );
    }
    return jdbcClient.sql("""
            WITH candidates AS (
                SELECT
                    d.id AS document_id,
                    dv.id AS document_version_id,
                    s.id AS section_id,
                    c.id AS chunk_id,
                    d.source_id,
                    d.external_document_id,
                    s.title AS section_title,
                    c.content,
                    1 - (
                        e.embedding
                        <=>
                        CAST(:queryEmbedding AS vector)
                    ) AS score
                FROM retrieval_chunk r
                JOIN document_chunk c
                  ON c.id = r.chunk_id
                JOIN document_section s
                  ON s.id = c.section_id
                JOIN document_version dv
                  ON dv.id = s.document_version_id
                JOIN document d
                  ON d.id = dv.document_id
                JOIN chunk_embedding e
                  ON e.chunk_id = c.id
                WHERE dv.status = 'ACTIVE'
                  AND e.provider = :provider
                  AND e.model = :model
                  AND e.dimensions = :dimensions
                  AND (
                      CAST(:sourceId AS VARCHAR) IS NULL
                      OR d.source_id = CAST(:sourceId AS VARCHAR)
                  )
            )
            SELECT
                document_id,
                document_version_id,
                section_id,
                chunk_id,
                source_id,
                external_document_id,
                section_title,
                content,
                score
            FROM candidates
            WHERE score >= :minScore
            ORDER BY
                score DESC,
                chunk_id
            LIMIT :limit
            """)
        .param(
            "queryEmbedding",
            new PGvector(queryEmbedding)
        )
        .param("provider", provider)
        .param("model", model)
        .param("dimensions", dimensions)
        .param("minScore", minScore)
        .param("sourceId", sourceId)
        .param("limit", limit)
        .query(this::mapHit)
        .list();
  }

  private RetrievalHit mapHit(
      ResultSet rs,
      int rowNum
  ) throws SQLException {
    return new RetrievalHit(
        rs.getObject(
            "document_id",
            UUID.class
        ),
        rs.getObject(
            "document_version_id",
            UUID.class
        ),
        rs.getObject(
            "section_id",
            UUID.class
        ),
        rs.getObject(
            "chunk_id",
            UUID.class
        ),
        rs.getString(
            "source_id"
        ),
        rs.getString(
            "external_document_id"
        ),
        rs.getString(
            "section_title"
        ),
        rs.getString(
            "content"
        ),
        rs.getDouble(
            "score"
        )
    );
  }
}