package com.ansaradd.workflowragsrc.retrieval.persistence.postgres;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.repository.LexicalRetrievalSearchRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresLexicalRetrievalSearchRepository
    implements LexicalRetrievalSearchRepository {

  private final JdbcClient jdbcClient;

  public PostgresLexicalRetrievalSearchRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public List<RetrievalHit> search(
      String query,
      List<String> sourceIds,
      int limit
  ) {
    Objects.requireNonNull(
        query,
        "query must not be null"
    );
    Objects.requireNonNull(
        sourceIds,
        "sourceIds must not be null"
    );

    String normalizedQuery =
        query.strip();

    if (normalizedQuery.isEmpty()) {
      throw new IllegalArgumentException(
          "query must not be blank"
      );
    }

    if (limit <= 0) {
      throw new IllegalArgumentException(
          "limit must be positive"
      );
    }

    String sourceFilter =
        sourceIds.isEmpty()
            ? ""
            : "AND d.source_id IN (:sourceIds)";

    String sql = """
        WITH lexical_query AS (
            SELECT websearch_to_tsquery(
                'simple'::regconfig,
                :query
            ) AS value
        )
        SELECT
            d.id AS document_id,
            dv.id AS document_version_id,
            s.id AS section_id,
            c.id AS chunk_id,
            d.source_id,
            d.external_document_id,
            s.title AS section_title,
            c.content,
            ts_rank_cd(
                r.search_vector,
                q.value,
                32
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
        CROSS JOIN lexical_query q
        WHERE dv.status = 'ACTIVE'
          AND r.search_vector @@ q.value
          %s
        ORDER BY
            score DESC,
            chunk_id
        LIMIT :limit
        """.formatted(sourceFilter);

    JdbcClient.StatementSpec statement =
        jdbcClient.sql(sql)
            .param(
                "query",
                normalizedQuery
            )
            .param(
                "limit",
                limit
            );

    if (!sourceIds.isEmpty()) {
      statement =
          statement.param(
              "sourceIds",
              sourceIds
          );
    }

    return statement
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