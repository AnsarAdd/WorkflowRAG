package com.ansaradd.workflowragsrc.chunk.persistence.postgres;

import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.chunk.repository.ChunkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresChunkRepository
    implements ChunkRepository {

  private final JdbcClient jdbcClient;

  public PostgresChunkRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public void replaceForVersion(
      UUID documentVersionId,
      String processingFingerprint,
      List<Chunk> chunks
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );
    Objects.requireNonNull(
        processingFingerprint,
        "processingFingerprint must not be null"
    );
    Objects.requireNonNull(
        chunks,
        "chunks must not be null"
    );

    Set<UUID> sectionIds =
        findSectionIds(
            documentVersionId
        );

    validateChunks(
        chunks,
        sectionIds
    );

    /*
     * Stable chunk IDs mean that unchanged logical chunks
     * update the existing row instead of being deleted.
     *
     * Existing chunk_embedding rows therefore survive.
     */
    for (Chunk chunk : chunks) {
      upsertChunk(
          chunk,
          processingFingerprint
      );
    }

    /*
     * Physically remove only chunks which no longer exist
     * in the current chunking result.
     *
     * Their embeddings are removed by FK cascade.
     */
    deleteStaleChunks(
        documentVersionId,
        chunks
    );
  }

  @Override
  public Optional<Chunk> findById(
      UUID id
  ) {
    Objects.requireNonNull(
        id,
        "id must not be null"
    );

    return jdbcClient.sql("""
            SELECT
                c.id,
                c.section_id,
                c.chunk_index,
                c.content,
                c.content_hash,
                c.split_section
            FROM document_chunk c
            WHERE c.id = :id
            """)
        .param(
            "id",
            id
        )
        .query(this::mapChunk)
        .optional();
  }

  @Override
  public List<Chunk> findNeighbors(
      UUID sectionId,
      int chunkIndex,
      int distance
  ) {
    Objects.requireNonNull(
        sectionId,
        "sectionId must not be null"
    );

    if (chunkIndex < 0) {
      throw new IllegalArgumentException(
          "chunkIndex must not be negative: "
              + chunkIndex
      );
    }

    if (distance < 0) {
      throw new IllegalArgumentException(
          "distance must not be negative: "
              + distance
      );
    }

    int fromIndex =
        Math.max(
            0,
            chunkIndex - distance
        );

    int toIndex =
        (int) Math.min(
            Integer.MAX_VALUE,
            (long) chunkIndex + distance
        );

    return jdbcClient.sql("""
            SELECT
                c.id,
                c.section_id,
                c.chunk_index,
                c.content,
                c.content_hash,
                c.split_section
            FROM document_chunk c
            WHERE c.section_id = :sectionId
              AND c.chunk_index BETWEEN :fromIndex AND :toIndex
            ORDER BY c.chunk_index
            """)
        .param(
            "sectionId",
            sectionId
        )
        .param(
            "fromIndex",
            fromIndex
        )
        .param(
            "toIndex",
            toIndex
        )
        .query(this::mapChunk)
        .list();
  }

  @Override
  public boolean hasReusableChunking(
      UUID documentVersionId,
      String processingFingerprint
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );
    Objects.requireNonNull(
        processingFingerprint,
        "processingFingerprint must not be null"
    );

    return jdbcClient.sql("""
            SELECT
                EXISTS (
                    SELECT 1
                    FROM document_chunk c
                    JOIN document_section s
                      ON s.id = c.section_id
                    WHERE s.document_version_id = :documentVersionId
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM document_chunk c
                    JOIN document_section s
                      ON s.id = c.section_id
                    WHERE s.document_version_id = :documentVersionId
                      AND c.processing_fingerprint <> :processingFingerprint
                )
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .param(
            "processingFingerprint",
            processingFingerprint
        )
        .query(Boolean.class)
        .single();
  }

  @Override
  public List<Chunk> findByDocumentVersionId(
      UUID documentVersionId
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );

    return jdbcClient.sql("""
            SELECT
                c.id,
                c.section_id,
                c.chunk_index,
                c.content,
                c.content_hash,
                c.split_section
            FROM document_chunk c
            JOIN document_section s
              ON s.id = c.section_id
            WHERE s.document_version_id = :documentVersionId
            ORDER BY
                s.section_order,
                c.chunk_index
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .query(this::mapChunk)
        .list();
  }

  private void upsertChunk(
      Chunk chunk,
      String processingFingerprint
  ) {
    int updated = jdbcClient.sql("""
            INSERT INTO document_chunk (
                id,
                section_id,
                chunk_index,
                content,
                content_hash,
                processing_fingerprint,
                split_section
            )
            VALUES (
                :id,
                :sectionId,
                :chunkIndex,
                :content,
                :contentHash,
                :processingFingerprint,
                :splitSection
            )
            ON CONFLICT (id)
            DO UPDATE SET
                section_id = EXCLUDED.section_id,
                chunk_index = EXCLUDED.chunk_index,
                content = EXCLUDED.content,
                content_hash = EXCLUDED.content_hash,
                processing_fingerprint =
                    EXCLUDED.processing_fingerprint,
                split_section = EXCLUDED.split_section
            """)
        .param("id", chunk.id())
        .param(
            "sectionId",
            chunk.sectionId()
        )
        .param(
            "chunkIndex",
            chunk.chunkIndex()
        )
        .param(
            "content",
            chunk.content()
        )
        .param(
            "contentHash",
            chunk.contentHash()
        )
        .param(
            "processingFingerprint",
            processingFingerprint
        )
        .param(
            "splitSection",
            chunk.splitSection()
        )
        .update();

    if (updated != 1) {
      throw new IllegalStateException(
          "Failed to upsert chunk: "
              + chunk.id()
      );
    }
  }

  private void deleteStaleChunks(
      UUID documentVersionId,
      List<Chunk> chunks
  ) {
    Set<UUID> targetIds =
        new HashSet<>();

    for (Chunk chunk : chunks) {
      targetIds.add(
          chunk.id()
      );
    }

    List<UUID> existingIds =
        jdbcClient.sql("""
                SELECT c.id
                FROM document_chunk c
                JOIN document_section s
                  ON s.id = c.section_id
                WHERE s.document_version_id = :documentVersionId
                """)
            .param(
                "documentVersionId",
                documentVersionId
            )
            .query(UUID.class)
            .list();

    List<UUID> staleIds =
        existingIds.stream()
            .filter(
                id -> !targetIds.contains(id)
            )
            .toList();

    for (UUID staleId : staleIds) {
      jdbcClient.sql("""
              DELETE FROM document_chunk
              WHERE id = :id
              """)
          .param(
              "id",
              staleId
          )
          .update();
    }
  }

  private Set<UUID> findSectionIds(
      UUID documentVersionId
  ) {
    List<UUID> ids =
        jdbcClient.sql("""
                SELECT id
                FROM document_section
                WHERE document_version_id = :documentVersionId
                """)
            .param(
                "documentVersionId",
                documentVersionId
            )
            .query(UUID.class)
            .list();

    return Set.copyOf(ids);
  }

  private void validateChunks(
      List<Chunk> chunks,
      Set<UUID> sectionIds
  ) {
    Set<UUID> chunkIds =
        new HashSet<>();

    Set<String> positions =
        new HashSet<>();

    for (Chunk chunk : chunks) {
      Objects.requireNonNull(
          chunk,
          "chunk must not be null"
      );

      if (!chunkIds.add(
          chunk.id()
      )) {
        throw new IllegalArgumentException(
            "Duplicate chunk id: "
                + chunk.id()
        );
      }

      if (!sectionIds.contains(
          chunk.sectionId()
      )) {
        throw new IllegalArgumentException(
            "Chunk "
                + chunk.id()
                + " references section "
                + chunk.sectionId()
                + " outside target document version"
        );
      }

      if (chunk.chunkIndex() < 0) {
        throw new IllegalArgumentException(
            "Chunk index must not be negative: "
                + chunk.chunkIndex()
        );
      }

      String position =
          chunk.sectionId()
              + ":"
              + chunk.chunkIndex();

      if (!positions.add(position)) {
        throw new IllegalArgumentException(
            "Duplicate chunk position: "
                + position
        );
      }
    }
  }

  private Chunk mapChunk(
      ResultSet rs,
      int rowNum
  ) throws SQLException {
    return new Chunk(
        rs.getObject(
            "id",
            UUID.class
        ),
        rs.getObject(
            "section_id",
            UUID.class
        ),
        rs.getInt(
            "chunk_index"
        ),
        rs.getString(
            "content"
        ),
        rs.getString(
            "content_hash"
        ),
        rs.getBoolean(
            "split_section"
        )
    );
  }
}