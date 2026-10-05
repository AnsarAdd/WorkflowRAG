package com.ansaradd.workflowragsrc.embedding.persistence.postgres;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ChunkEmbeddingReuseTest {
  @Container
  static final PostgreSQLContainer postgres = new PostgreSQLContainer(
      DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
  private JdbcClient db;
  private PostgresChunkEmbeddingRepository repository;

  @BeforeEach
  void initialize() throws Exception {
    var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var migration = new SpringLiquibase();
    migration.setDataSource(source);
    migration.setChangeLog("classpath:db/changelog/changelog-master.yml");
    migration.setResourceLoader(new DefaultResourceLoader());
    migration.afterPropertiesSet();
    db = JdbcClient.create(source);
    db.sql("TRUNCATE document CASCADE").update();
    repository = new PostgresChunkEmbeddingRepository(db);
  }

  @Test
  void reusesExactFingerprintAfterSectionMovementAndChunkInsertion() {
    UUID document = document();
    UUID active = version(document, 1, "ACTIVE");
    UUID target = version(document, 2, "BUILDING");
    UUID original = chunk(active, "old-position", 0);
    embedding(original, "a".repeat(64), 1);
    UUID moved = chunk(target, "new-position", 3);
    assertTrue(repository.reuseFromActiveVersion(moved, "a".repeat(64)));
    assertEquals("1", db.sql("SELECT (embedding::real[])[1]::text FROM chunk_embedding WHERE chunk_id=:id")
        .param("id", moved).query(String.class).single());
    assertFalse(repository.reuseFromActiveVersion(moved, "b".repeat(64)),
        "Changed model revision or heading context changes the fingerprint");
  }

  @Test
  void duplicateExactMatchesProduceOnlyOneDeterministicCopy() {
    UUID document = document();
    UUID active = version(document, 1, "ACTIVE");
    embedding(chunk(active, "one", 0), "a".repeat(64), 1);
    embedding(chunk(active, "two", 0), "a".repeat(64), 2);
    UUID moved = chunk(version(document, 2, "BUILDING"), "three", 5);
    assertTrue(repository.reuseFromActiveVersion(moved, "a".repeat(64)));
    String vector = db.sql("SELECT embedding::text FROM chunk_embedding WHERE chunk_id=:id")
        .param("id", moved).query(String.class).single();
    assertTrue(repository.reuseFromActiveVersion(moved, "a".repeat(64)));
    assertEquals(vector, db.sql("SELECT embedding::text FROM chunk_embedding WHERE chunk_id=:id")
        .param("id", moved).query(String.class).single());
  }

  @Test
  void neverReusesAnotherDocumentOrInactiveVersion() {
    embedding(chunk(version(document(), 1, "ACTIVE"), "same", 0), "a".repeat(64), 1);
    UUID document = document();
    embedding(chunk(version(document, 1, "INACTIVE"), "same", 0), "a".repeat(64), 1);
    UUID target = chunk(version(document, 2, "BUILDING"), "same", 0);
    assertFalse(repository.reuseFromActiveVersion(target, "a".repeat(64)));
  }

  private UUID document() {
    UUID id = UUID.randomUUID();
    db.sql("INSERT INTO document(id, source_id, external_document_id) VALUES (:id, 'test', :external)")
        .param("id", id).param("external", id.toString()).update();
    return id;
  }

  private UUID version(UUID document, int number, String status) {
    UUID id = UUID.randomUUID();
    db.sql("""
        INSERT INTO document_version(id,document_id,version_number,format,content_hash,content,status,activated_at)
        VALUES (:id,:document,:number,'MARKDOWN',repeat('a',64),decode('00','hex'),:status,
          CASE WHEN :status IN ('ACTIVE','INACTIVE') THEN now() ELSE NULL END)
        """).param("id", id).param("document", document).param("number", number).param("status", status).update();
    return id;
  }

  private UUID chunk(UUID version, String key, int index) {
    UUID section = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    int order = db.sql("SELECT count(*) FROM document_section WHERE document_version_id=:version")
        .param("version", version).query(Integer.class).single();
    db.sql("""
        INSERT INTO document_section(id,document_version_id,stable_key,section_order,title,level,content,content_hash,processing_fingerprint)
        VALUES (:id,:version,:key,:ordering,'Title',1,'same paragraph',repeat('b',64),repeat('c',64))
        """).param("id", section).param("version", version).param("key", key).param("ordering", order).update();
    db.sql("""
        INSERT INTO document_chunk(id,section_id,chunk_index,content,content_hash,processing_fingerprint,split_section)
        VALUES (:id,:section,:index,'same paragraph',repeat('d',64),repeat('e',64),true)
        """).param("id", id).param("section", section).param("index", index).update();
    return id;
  }

  private void embedding(UUID chunk, String fingerprint, int value) {
    db.sql("""
        INSERT INTO chunk_embedding(id,chunk_id,provider,model,model_revision,dimensions,processing_fingerprint,embedding)
        VALUES (:id,:chunk,'ollama','bge-m3','revision-1',1024,:fingerprint,array_fill(CAST(:value AS real),ARRAY[1024])::vector)
        """).param("id", UUID.randomUUID()).param("chunk", chunk)
        .param("fingerprint", fingerprint).param("value", value).update();
  }
}
