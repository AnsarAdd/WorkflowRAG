package com.ansaradd.workflowragsrc.document.persistence.postgres;

import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
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
public class PostgresSectionRepository
    implements SectionRepository {

  private final JdbcClient jdbcClient;

  public PostgresSectionRepository(
      JdbcClient jdbcClient
  ) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public void replaceForVersion(
      UUID documentVersionId,
      String processingFingerprint,
      List<Section> sections
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
        sections,
        "sections must not be null"
    );

    validateSections(
        documentVersionId,
        sections
    );

    /*
     * First pass:
     *
     * Insert/update every section without parent relation.
     *
     * Existing rows keep the same primary key, therefore their
     * document_chunk rows are not removed by FK cascade.
     */
    for (Section section : sections) {
      upsertWithoutParent(
          section,
          processingFingerprint
      );
    }

    /*
     * Second pass:
     *
     * All target sections now exist, so parent references
     * can safely be restored.
     */
    for (Section section : sections) {
      if (section.parentId() != null) {
        updateParent(section);
      }
    }

    /*
     * Only sections which disappeared from the parsed
     * document are physically removed.
     */
    deleteStaleSections(
        documentVersionId,
        sections
    );
  }

  @Override
  public boolean hasReusableParse(
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
                    FROM document_section
                    WHERE document_version_id = :documentVersionId
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM document_section
                    WHERE document_version_id = :documentVersionId
                      AND processing_fingerprint <> :processingFingerprint
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
  public List<Section> findByDocumentVersionId(
      UUID documentVersionId
  ) {
    Objects.requireNonNull(
        documentVersionId,
        "documentVersionId must not be null"
    );

    return jdbcClient.sql("""
            SELECT
                id,
                document_version_id,
                stable_key,
                parent_id,
                section_order,
                title,
                level,
                content,
                content_hash
            FROM document_section
            WHERE document_version_id = :documentVersionId
            ORDER BY section_order
            """)
        .param(
            "documentVersionId",
            documentVersionId
        )
        .query(this::mapSection)
        .list();
  }

  @Override
  public Optional<Section> findById(
      UUID id
  ) {
    Objects.requireNonNull(
        id,
        "id must not be null"
    );

    return jdbcClient.sql("""
          SELECT
              id,
              document_version_id,
              stable_key,
              parent_id,
              section_order,
              title,
              level,
              content,
              content_hash
          FROM document_section
          WHERE id = :id
          """)
        .param("id", id)
        .query(this::mapSection)
        .optional();
  }

  private void upsertWithoutParent(
      Section section,
      String processingFingerprint
  ) {
    int updated = jdbcClient.sql("""
            INSERT INTO document_section (
                id,
                document_version_id,
                stable_key,
                parent_id,
                section_order,
                title,
                level,
                content,
                content_hash,
                processing_fingerprint
            )
            VALUES (
                :id,
                :documentVersionId,
                :stableKey,
                NULL,
                :sectionOrder,
                :title,
                :level,
                :content,
                :contentHash,
                :processingFingerprint
            )
            ON CONFLICT (id)
            DO UPDATE SET
                stable_key = EXCLUDED.stable_key,
                parent_id = NULL,
                section_order = EXCLUDED.section_order,
                title = EXCLUDED.title,
                level = EXCLUDED.level,
                content = EXCLUDED.content,
                content_hash = EXCLUDED.content_hash,
                processing_fingerprint =
                    EXCLUDED.processing_fingerprint
            WHERE document_section.document_version_id =
                  EXCLUDED.document_version_id
            """)
        .param("id", section.id())
        .param(
            "documentVersionId",
            section.documentVersionId()
        )
        .param(
            "stableKey",
            section.stableKey()
        )
        .param(
            "sectionOrder",
            section.sectionOrder()
        )
        .param("title", section.title())
        .param("level", section.level())
        .param("content", section.content())
        .param(
            "contentHash",
            section.contentHash()
        )
        .param(
            "processingFingerprint",
            processingFingerprint
        )
        .update();

    if (updated != 1) {
      throw new IllegalStateException(
          "Failed to upsert section: "
              + section.id()
      );
    }
  }

  private void updateParent(
      Section section
  ) {
    int updated = jdbcClient.sql("""
            UPDATE document_section
            SET parent_id = :parentId
            WHERE id = :id
              AND document_version_id = :documentVersionId
            """)
        .param(
            "parentId",
            section.parentId()
        )
        .param(
            "id",
            section.id()
        )
        .param(
            "documentVersionId",
            section.documentVersionId()
        )
        .update();

    if (updated != 1) {
      throw new IllegalStateException(
          "Failed to update parent for section: "
              + section.id()
      );
    }
  }

  private void deleteStaleSections(
      UUID documentVersionId,
      List<Section> sections
  ) {
    Set<UUID> targetIds =
        new HashSet<>();

    for (Section section : sections) {
      targetIds.add(section.id());
    }

    List<UUID> existingIds =
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

    List<UUID> staleIds =
        existingIds.stream()
            .filter(id -> !targetIds.contains(id))
            .toList();

    if (staleIds.isEmpty()) {
      return;
    }

    /*
     * Break self-references between stale sections first.
     * Retained sections cannot reference stale sections because
     * validateSections() requires every parent to belong to the
     * current target set.
     */
    for (UUID staleId : staleIds) {
      jdbcClient.sql("""
              UPDATE document_section
              SET parent_id = NULL
              WHERE id = :id
                AND document_version_id = :documentVersionId
              """)
          .param("id", staleId)
          .param(
              "documentVersionId",
              documentVersionId
          )
          .update();
    }

    for (UUID staleId : staleIds) {
      jdbcClient.sql("""
              DELETE FROM document_section
              WHERE id = :id
                AND document_version_id = :documentVersionId
              """)
          .param("id", staleId)
          .param(
              "documentVersionId",
              documentVersionId
          )
          .update();
    }
  }

  private void validateSections(
      UUID documentVersionId,
      List<Section> sections
  ) {
    Set<UUID> ids =
        new HashSet<>();

    Set<String> stableKeys =
        new HashSet<>();

    for (Section section : sections) {
      Objects.requireNonNull(
          section,
          "section must not be null"
      );

      if (!documentVersionId.equals(
          section.documentVersionId()
      )) {
        throw new IllegalArgumentException(
            "Section "
                + section.id()
                + " belongs to another document version"
        );
      }

      if (!ids.add(section.id())) {
        throw new IllegalArgumentException(
            "Duplicate section id: "
                + section.id()
        );
      }

      if (!stableKeys.add(
          section.stableKey()
      )) {
        throw new IllegalArgumentException(
            "Duplicate section stable key: "
                + section.stableKey()
        );
      }

      if (section.id().equals(
          section.parentId()
      )) {
        throw new IllegalArgumentException(
            "Section cannot reference itself as parent: "
                + section.id()
        );
      }
    }

    for (Section section : sections) {
      if (section.parentId() != null
          && !ids.contains(
          section.parentId()
      )) {

        throw new IllegalArgumentException(
            "Section "
                + section.id()
                + " references parent outside target set: "
                + section.parentId()
        );
      }
    }
  }

  private Section mapSection(
      ResultSet rs,
      int rowNum
  ) throws SQLException {
    return new Section(
        rs.getObject(
            "id",
            UUID.class
        ),
        rs.getObject(
            "document_version_id",
            UUID.class
        ),
        rs.getString("stable_key"),
        rs.getObject(
            "parent_id",
            UUID.class
        ),
        rs.getInt("section_order"),
        rs.getString("title"),
        rs.getInt("level"),
        rs.getString("content"),
        rs.getString("content_hash")
    );
  }
}