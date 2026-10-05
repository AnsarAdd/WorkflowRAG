package com.ansaradd.workflowragsrc.embedding.persistence.postgres;

import com.ansaradd.workflowragsrc.embedding.model.ChunkEmbedding;
import com.ansaradd.workflowragsrc.embedding.repository.ChunkEmbeddingRepository;
import com.pgvector.PGvector;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresChunkEmbeddingRepository
    implements ChunkEmbeddingRepository {

  private final JdbcClient jdbcClient;

  public PostgresChunkEmbeddingRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public boolean reuseFromActiveVersion(
      UUID targetChunkId,
      String processingFingerprint
  ) {
    Objects.requireNonNull(
        targetChunkId,
        "targetChunkId must not be null"
    );
    Objects.requireNonNull(
        processingFingerprint,
        "processingFingerprint must not be null"
    );

    int updated = jdbcClient.sql("""
          INSERT INTO chunk_embedding (
              id,
              chunk_id,
              provider,
              model,
              model_revision,
              dimensions,
              processing_fingerprint,
              embedding,
              created_at
          )
          SELECT
              :embeddingId,
              target_chunk.id,
              source_embedding.provider,
              source_embedding.model,
              source_embedding.model_revision,
              source_embedding.dimensions,
              source_embedding.processing_fingerprint,
              source_embedding.embedding,
              CURRENT_TIMESTAMP
          FROM document_chunk target_chunk
          JOIN document_section target_section
            ON target_section.id = target_chunk.section_id
          JOIN document_version target_version
            ON target_version.id =
               target_section.document_version_id

          JOIN document_version source_version
            ON source_version.document_id =
               target_version.document_id
           AND source_version.status = 'ACTIVE'
           AND source_version.id <> target_version.id

          JOIN document_section source_section
            ON source_section.document_version_id =
               source_version.id

          JOIN document_chunk source_chunk
            ON source_chunk.section_id =
               source_section.id

          JOIN chunk_embedding source_embedding
            ON source_embedding.chunk_id =
               source_chunk.id
           AND source_embedding.processing_fingerprint =
               :processingFingerprint

          WHERE target_chunk.id = :targetChunkId
          ORDER BY source_chunk.id
          LIMIT 1

          ON CONFLICT (chunk_id)
          DO UPDATE SET
              provider = EXCLUDED.provider,
              model = EXCLUDED.model,
              model_revision = EXCLUDED.model_revision,
              dimensions = EXCLUDED.dimensions,
              processing_fingerprint =
                  EXCLUDED.processing_fingerprint,
              embedding = EXCLUDED.embedding,
              created_at = CURRENT_TIMESTAMP
          """)
        .param(
            "embeddingId",
            UUID.randomUUID()
        )
        .param(
            "targetChunkId",
            targetChunkId
        )
        .param(
            "processingFingerprint",
            processingFingerprint
        )
        .update();

    return updated == 1;
  }

  @Override
  public Map<UUID, String> findFingerprintsByDocumentVersionId(
      UUID documentVersionId
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );

    Map<UUID, String> fingerprints =
        new HashMap<>();

    jdbcClient.sql("""
            SELECT
                e.chunk_id,
                e.processing_fingerprint
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
        .query((rs, rowNum) -> {
          fingerprints.put(
              rs.getObject(
                  "chunk_id",
                  UUID.class
              ),
              rs.getString(
                  "processing_fingerprint"
              )
          );

          return 0;
        })
        .list();

    return Map.copyOf(fingerprints);
  }

  @Override
  public void upsert(
      ChunkEmbedding embedding
  ) {
    Objects.requireNonNull(
        embedding,
        "embedding must not be null"
    );

    int updated = jdbcClient.sql("""
            INSERT INTO chunk_embedding (
                id,
                chunk_id,
                provider,
                model,
                model_revision,
                dimensions,
                processing_fingerprint,
                embedding,
                created_at
            )
            VALUES (
                :id,
                :chunkId,
                :provider,
                :model,
                :modelRevision,
                :dimensions,
                :processingFingerprint,
                :embedding,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (chunk_id)
            DO UPDATE SET
                id = EXCLUDED.id,
                provider = EXCLUDED.provider,
                model = EXCLUDED.model,
                model_revision = EXCLUDED.model_revision,
                dimensions = EXCLUDED.dimensions,
                processing_fingerprint =
                    EXCLUDED.processing_fingerprint,
                embedding = EXCLUDED.embedding,
                created_at = CURRENT_TIMESTAMP
            """)
        .param("id", embedding.id())
        .param("chunkId", embedding.chunkId())
        .param("provider", embedding.provider())
        .param("model", embedding.model())
        .param(
            "modelRevision",
            embedding.modelRevision()
        )
        .param(
            "dimensions",
            embedding.dimensions()
        )
        .param(
            "processingFingerprint",
            embedding.processingFingerprint()
        )
        .param(
            "embedding",
            new PGvector(embedding.vector())
        )
        .update();

    if (updated != 1) {
      throw new IllegalStateException(
          "Failed to upsert embedding for chunk: "
              + embedding.chunkId()
      );
    }
  }
}