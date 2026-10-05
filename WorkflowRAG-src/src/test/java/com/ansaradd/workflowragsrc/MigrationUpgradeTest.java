package com.ansaradd.workflowragsrc;

import static org.junit.jupiter.api.Assertions.*;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class MigrationUpgradeTest {
  @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer(
      DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void upgradesKnownSchemasWithoutLosingIndexedDocuments(boolean editedSchema) throws Exception {
    var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    migrate(source, "classpath:legacy/db/changelog/changelog-master.yml");
    var db = JdbcClient.create(source);
    db.sql("TRUNCATE document CASCADE").update();
    if (editedSchema) {
      db.sql("ALTER TABLE chunk_embedding ALTER COLUMN model_revision SET NOT NULL").update();
      db.sql("ALTER TABLE retrieval_chunk ADD COLUMN search_vector tsvector NOT NULL").update();
      db.sql("CREATE INDEX ix_retrieval_chunk_search_vector ON retrieval_chunk USING GIN(search_vector)").update();
      db.sql("UPDATE databasechangelog SET md5sum='9:61cca937e3e30e8e444d37102c7a6fa9' WHERE id='005-create-chunk-embedding'").update();
      db.sql("UPDATE databasechangelog SET md5sum='9:6bf85b753619c3a50399dcbb436880cc' WHERE id='006-create-retrieval-chunk'").update();
    }
    db.sql("INSERT INTO document(id,source_id,external_document_id) VALUES ('00000000-0000-0000-0000-000000000001','legacy','saved.md')").update();
    db.sql("INSERT INTO document_version(id,document_id,version_number,format,content_hash,content,status,activated_at) VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001',1,'MARKDOWN',repeat('a',64),decode('00','hex'),'ACTIVE',now())").update();
    db.sql("INSERT INTO document_section(id,document_version_id,stable_key,section_order,title,level,content,content_hash,processing_fingerprint) VALUES ('00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000002','payments',0,'Payments',1,'Kafka transactions',repeat('b',64),repeat('c',64))").update();
    db.sql("INSERT INTO document_chunk(id,section_id,chunk_index,content,content_hash,processing_fingerprint,split_section) VALUES ('00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000003',0,'Kafka transactions',repeat('d',64),repeat('e',64),false)").update();
    db.sql("INSERT INTO chunk_embedding(id,chunk_id,provider,model,model_revision,dimensions,processing_fingerprint,embedding) VALUES ('00000000-0000-0000-0000-000000000005','00000000-0000-0000-0000-000000000004','ollama','bge-m3','',1024,repeat('f',64),array_fill(0::real,ARRAY[1024])::vector)").update();
    db.sql(editedSchema
        ? "INSERT INTO retrieval_chunk(chunk_id,search_vector) VALUES ('00000000-0000-0000-0000-000000000004',to_tsvector('simple','Payments Kafka transactions'))"
        : "INSERT INTO retrieval_chunk(chunk_id) VALUES ('00000000-0000-0000-0000-000000000004')").update();
    migrate(source, "classpath:db/changelog/changelog-master.yml");
    assertEquals("saved.md", db.sql("SELECT external_document_id FROM document").query(String.class).single());
    assertEquals(1, db.sql("SELECT count(*) FROM information_schema.columns WHERE table_name='retrieval_chunk' AND column_name='search_vector'").query(Integer.class).single());
    assertEquals(1, db.sql("SELECT count(*) FROM pg_indexes WHERE indexname='ix_retrieval_chunk_search_vector'").query(Integer.class).single());
    assertEquals(1, db.sql("SELECT count(*) FROM retrieval_chunk WHERE search_vector @@ websearch_to_tsquery('simple','Kafka')").query(Integer.class).single());
    assertEquals("", db.sql("SELECT model_revision FROM chunk_embedding").query(String.class).single());
    migrate(source, "classpath:db/changelog/changelog-master.yml");
    // Each variant starts from the original schema in its own freshly reset namespace.
    db.sql("DROP SCHEMA public CASCADE").update();
    db.sql("CREATE SCHEMA public").update();
  }

  private void migrate(DriverManagerDataSource source, String changelog) throws Exception {
    var migration = new SpringLiquibase();
    migration.setDataSource(source);
    migration.setChangeLog(changelog);
    migration.setResourceLoader(new DefaultResourceLoader());
    migration.afterPropertiesSet();
  }
}
