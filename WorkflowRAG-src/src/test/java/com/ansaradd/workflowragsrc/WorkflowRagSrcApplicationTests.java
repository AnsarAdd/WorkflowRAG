package com.ansaradd.workflowragsrc;

import static org.junit.jupiter.api.Assertions.*;

import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
import com.ansaradd.workflowragsrc.workflow.recovery.WorkflowRecoveryRunner;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"workflowrag.reranking.enabled=false", "workflowrag.source-routing.enabled=false"})
@Import(WorkflowRagSrcApplicationTests.Models.class)
class WorkflowRagSrcApplicationTests {
  @Container
  static final PostgreSQLContainer postgres = new PostgreSQLContainer(
      DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
  @TempDir static Path documents;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("workflowrag.sources[0].id", () -> "test-files");
    r.add("workflowrag.sources[0].type", () -> "LOCAL_FILE");
    r.add("workflowrag.sources[0].name", () -> "Test files");
    r.add("workflowrag.sources[0].update-policy", () -> "AUTHORITATIVE_SNAPSHOT");
    r.add("workflowrag.sources[0].pipeline-id", () -> "default-ingestion");
    r.add("workflowrag.sources[0].configuration.root", () -> documents.toString());
  }

  @LocalServerPort int port;
  @Autowired JdbcClient db;
  @Autowired ObjectMapper json;
  @Autowired TestEmbeddings embeddings;
  @Autowired WorkflowExecutor executor;
  @Autowired WorkflowRecoveryRunner recovery;
  final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach void reset() {
    db.sql("TRUNCATE ingestion_preview, document, job CASCADE").update();
    embeddings.fail = false;
    embeddings.calls = 0;
    embeddings.revision = "test-v1";
  }

  @Test void ingestionRunsPipelineAndSupportsAllSearchProfiles() throws Exception {
    var response = ingest("hello.md", "# Payments\n\nPayments use Kafka transactions.");
    assertEquals(200, response.statusCode(), response.body());
    assertEquals("ACTIVE", scalar("SELECT status FROM document_version"));
    assertEquals("COMPLETED", scalar("SELECT status FROM job"));
    assertEquals(5, count("SELECT count(*) FROM job_step WHERE status IN ('SUCCEEDED','SKIPPED')"));
    assertEquals("COMPLETED", json.readTree(response.body()).path("status").asText());
    assertFalse(json.readTree(response.body()).path("jobId").asText().isBlank());
    for (String profile : List.of("SIMPLE", "HYBRID")) {
      var search = post("/api/v1/retrieval/search", Map.of("query", "Payments", "profile", profile));
      assertEquals(200, search.statusCode(), search.body());
      assertFalse(json.readTree(search.body()).path("hits").isEmpty(), search.body());
    }
    var advanced = post("/api/v1/retrieval/search", Map.of("query", "Payments", "profile", "ADVANCED"));
    assertEquals(503, advanced.statusCode(), advanced.body());
    assertTrue(advanced.body().contains("RERANKING_UNAVAILABLE"), advanced.body());
    assertEquals(400, get("/api/v1/jobs/not-a-uuid").statusCode());
    var context = post("/api/v1/retrieval/context", Map.of("query", "Payments", "profile", "HYBRID"));
    assertEquals(200, context.statusCode(), context.body());
    assertTrue(context.body().contains("Kafka transactions"), context.body());
    var job = get("/api/v1/jobs/" + scalar("SELECT id::text FROM job"));
    assertEquals(200, job.statusCode(), job.body());
    assertEquals(5, json.readTree(job.body()).path("steps").size());
  }

  @Test void unchangedInputSkipsAndNewVersionReusesUnchangedEmbeddings() throws Exception {
    assertEquals(200, ingest("versions.md", "# One\n\nSame paragraph.\n\n# Two\n\nOld text.").statusCode());
    int firstCalls = embeddings.calls;
    var same = post("/api/v1/ingestions/source", request("versions.md"));
    assertEquals(200, same.statusCode(), same.body());
    assertEquals("UNCHANGED", json.readTree(same.body()).path("status").asText());
    assertEquals(firstCalls, embeddings.calls);
    assertEquals(1, count("SELECT count(*) FROM job"));
    assertEquals(200, ingest("versions.md", "# One\n\nSame paragraph.\n\n# Two\n\nNew text.").statusCode());
    assertEquals(firstCalls + 1, embeddings.calls);
    assertEquals(1, count("SELECT count(*) FROM document_version WHERE status='ACTIVE'"));
    assertEquals(1, count("SELECT count(*) FROM document_version WHERE status='INACTIVE'"));
  }

  @Test void failedVersionPreservesActiveKnowledgeAndCanResume() throws Exception {
    assertEquals(200, ingest("failure.md", "# Payments\n\nOld working content.").statusCode());
    String active = scalar("SELECT id::text FROM document_version WHERE status='ACTIVE'");
    embeddings.fail = true;
    var failed = ingest("failure.md", "# Payments\n\nChanged content.");
    assertEquals(500, failed.statusCode(), failed.body());
    assertEquals(1, count("SELECT count(*) FROM document_version WHERE status='FAILED'"));
    assertEquals(active, scalar("SELECT id::text FROM document_version WHERE status='ACTIVE'"));
    String jobId = scalar("SELECT id::text FROM job WHERE status='FAILED'");
    embeddings.fail = false;
    var resumed = post("/api/v1/jobs/" + jobId + "/retry", Map.of());
    assertEquals(200, resumed.statusCode(), resumed.body());
    assertEquals("COMPLETED", scalar("SELECT status FROM job WHERE id='" + jobId + "'"));
    assertEquals(1, count("SELECT attempt FROM job_step WHERE job_id='" + jobId + "' AND stage='parse'"));
    assertEquals(2, count("SELECT attempt FROM job_step WHERE job_id='" + jobId + "' AND stage='embed'"));
    assertNotEquals(active, scalar("SELECT id::text FROM document_version WHERE status='ACTIVE'"));
  }

  @Test void failedOldJobCannotReplaceANewerVersion() throws Exception {
    embeddings.fail = true;
    assertEquals(500, ingest("newer.md", "# One\n\nFirst content.").statusCode());
    String oldJob = scalar("SELECT id::text FROM job");
    embeddings.fail = false;
    assertEquals(200, ingest("newer.md", "# One\n\nNew content.").statusCode());
    String active = scalar("SELECT id::text FROM document_version WHERE status='ACTIVE'");
    var retry = post("/api/v1/jobs/" + oldJob + "/retry", Map.of());
    assertEquals(409, retry.statusCode(), retry.body());
    assertEquals(active, scalar("SELECT id::text FROM document_version WHERE status='ACTIVE'"));
    assertEquals("FAILED", scalar("SELECT status FROM job WHERE id='" + oldJob + "'"));
  }

  @Test void semanticSearchDoesNotMixModelRevisions() throws Exception {
    assertEquals(200, ingest("revision.md", "# Payments\n\nPayment processing.").statusCode());
    embeddings.revision = "test-v2";
    var search = post("/api/v1/retrieval/search", Map.of("query", "Payments", "profile", "SIMPLE"));
    assertEquals(200, search.statusCode(), search.body());
    assertTrue(json.readTree(search.body()).path("hits").isEmpty(), search.body());
  }

  @Test void recoveryReleasesAnInterruptedBuild() throws Exception {
    embeddings.fail = true;
    ingest("interrupted.md", "# One\n\nInterrupted content.");
    db.sql("UPDATE document_version SET status='BUILDING'").update();
    db.sql("UPDATE job SET status='PENDING'").update();
    recovery.run(null);
    assertEquals("FAILED", scalar("SELECT status FROM document_version"));
    assertEquals("FAILED", scalar("SELECT status FROM job"));
    embeddings.fail = false;
    assertEquals(200, ingest("interrupted.md", "# One\n\nReplacement content.").statusCode());
  }

  @Test void explicitReindexRebuildsUnchangedContentForNewModelRevision() throws Exception {
    assertEquals(200, ingest("reindex.md", "# Payments\n\nPayment processing.").statusCode());
    int before = embeddings.calls;
    embeddings.revision = "test-v2";
    var response = post("/api/v1/ingestions/source", Map.of(
        "sourceId", "test-files", "externalDocumentId", "reindex.md",
        "parameters", Map.of("forceReindex", "true")));
    assertEquals(200, response.statusCode(), response.body());
    assertEquals("COMPLETED", json.readTree(response.body()).path("status").asText());
    assertEquals(before + 1, embeddings.calls);
    assertEquals(2, count("SELECT count(*) FROM document_version"));
    assertEquals("test-v2", scalar("SELECT e.model_revision FROM chunk_embedding e JOIN document_chunk c ON c.id=e.chunk_id JOIN document_section s ON s.id=c.section_id JOIN document_version v ON v.id=s.document_version_id WHERE v.status='ACTIVE'"));
  }
  @Test void previewValidatesBeforeSearchAndDoesNotCreateAJob() throws Exception {
    Files.write(documents.resolve("invalid.txt"), new byte[]{(byte) 0xc3, 0x28});
    var invalid = post("/api/v1/ingestions/previews/source", request("invalid.txt"));
    assertEquals(422, invalid.statusCode(), invalid.body());
    assertEquals(0, embeddings.calls);
    assertEquals(0, count("SELECT count(*) FROM job"));
    Files.writeString(documents.resolve("empty.txt"), "   \n");
    assertEquals(422, post("/api/v1/ingestions/source", request("empty.txt")).statusCode());
    assertEquals(0, count("SELECT count(*) FROM job"));
  }

  @Test void confirmationUsesSnapshotAndRepeatedConfirmationReturnsSameJob() throws Exception {
    Files.writeString(documents.resolve("snapshot.md"), "# Snapshot\n\nOriginal checked content.");
    var preview = post("/api/v1/ingestions/previews/source", request("snapshot.md"));
    assertEquals(200, preview.statusCode(), preview.body());
    String id = json.readTree(preview.body()).path("id").asText();
    assertEquals("READY", json.readTree(preview.body()).path("status").asText());
    assertEquals(0, count("SELECT count(*) FROM document_version"));
    assertEquals(0, count("SELECT count(*) FROM job"));
    Files.writeString(documents.resolve("snapshot.md"), "# Changed\n\nUnchecked replacement.");
    var confirmed = post("/api/v1/ingestions/previews/" + id + "/confirm", Map.of());
    assertEquals(200, confirmed.statusCode(), confirmed.body());
    String jobId = json.readTree(confirmed.body()).path("jobId").asText();
    assertFalse(jobId.isBlank());
    assertTrue(scalar("SELECT content FROM document_chunk").contains("Original checked"));
    var again = post("/api/v1/ingestions/previews/" + id + "/confirm", Map.of());
    assertEquals(200, again.statusCode(), again.body());
    assertEquals(jobId, json.readTree(again.body()).path("jobId").asText());
    assertEquals(1, count("SELECT count(*) FROM job"));
    assertEquals("CONFIRMED", json.readTree(get("/api/v1/ingestions/previews/" + id).body()).path("status").asText());
  }

  @Test void unchangedPreviewSkipsSearchAndStalePreviewCannotOverwriteNewerVersion() throws Exception {
    assertEquals(200, ingest("stale.md", "# Topic\n\nFirst version.").statusCode());
    int calls = embeddings.calls;
    var unchanged = post("/api/v1/ingestions/previews/source", request("stale.md"));
    assertEquals(200, unchanged.statusCode(), unchanged.body());
    assertEquals("UNCHANGED", json.readTree(unchanged.body()).path("status").asText());
    assertEquals(calls, embeddings.calls);
    Files.writeString(documents.resolve("stale.md"), "# Topic\n\nProposed version.");
    var proposal = post("/api/v1/ingestions/previews/source", request("stale.md"));
    assertEquals(200, proposal.statusCode(), proposal.body());
    String id = json.readTree(proposal.body()).path("id").asText();
    assertEquals(200, ingest("stale.md", "# Topic\n\nAnother committed version.").statusCode());
    var stale = post("/api/v1/ingestions/previews/" + id + "/confirm", Map.of());
    assertEquals(409, stale.statusCode(), stale.body());
    assertEquals(2, count("SELECT count(*) FROM job"));
  }

  @Test void similarityOutageIsExplicitAndExpiredPreviewIsRejected() throws Exception {
    assertEquals(200, ingest("existing.md", "# Kafka\n\nKafka transaction handling.").statusCode());
    embeddings.fail = true;
    Files.writeString(documents.resolve("outage.md"), "# Kafka\n\nKafka transaction changes.");
    var preview = post("/api/v1/ingestions/previews/source", request("outage.md"));
    assertEquals(200, preview.statusCode(), preview.body());
    assertEquals("PARTIAL", json.readTree(preview.body()).path("similarity").path("status").asText());
    String id = json.readTree(preview.body()).path("id").asText();
    db.sql("UPDATE ingestion_preview SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id=:id")
        .param("id", UUID.fromString(id)).update();
    var expired = post("/api/v1/ingestions/previews/" + id + "/confirm", Map.of());
    assertEquals(410, expired.statusCode(), expired.body());
    assertEquals(1, count("SELECT count(*) FROM job"));
  }

  @Test void insertingAndMovingParagraphsOnlyEmbedsNewContent() throws Exception {
    String first = "Alpha unchanged.\n\nBeta unchanged.\n\nGamma unchanged.";
    assertEquals(200, ingest("parts.txt", first).statusCode());
    int calls = embeddings.calls;
    assertEquals(3, calls);
    assertEquals(200, ingest("parts.txt", "Inserted paragraph.\n\n" + first).statusCode());
    assertEquals(calls + 1, embeddings.calls);
    assertEquals(200, ingest("parts.txt", "Gamma unchanged.\n\nAlpha   unchanged.\n\nBeta\nunchanged.\n\nInserted paragraph.").statusCode());
    assertEquals(calls + 1, embeddings.calls);
    Files.writeString(documents.resolve("parts.txt"), "Gamma unchanged.\n\nAlpha unchanged.\n\nBeta modified.\n\nInserted paragraph.");
    var preview = post("/api/v1/ingestions/previews/source", request("parts.txt"));
    assertEquals(200, preview.statusCode(), preview.body());
    var changes = json.readTree(preview.body()).path("changes");
    assertEquals(3, changes.path("unchangedParts").asInt());
    assertEquals(1, changes.path("addedParts").asInt());
    assertEquals(1, changes.path("removedParts").asInt());
    int beforeConfirm = embeddings.calls;
    String id = json.readTree(preview.body()).path("id").asText();
    assertEquals(200, post("/api/v1/ingestions/previews/" + id + "/confirm", Map.of()).statusCode());
    assertEquals(beforeConfirm + 1, embeddings.calls);
  }

  @Test void uploadPreviewFindsRelatedDocumentsAndConfirmsWithoutSourceFile() throws Exception {
    assertEquals(200, ingest("related.txt", "Kafka transactions.").statusCode());
    String boundary = "FinchUploadBoundary";
    String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"sourceId\"\r\n\r\ntest-files\r\n"
        + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"externalDocumentId\"\r\n\r\nuploaded.md\r\n"
        + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"uploaded.md\"\r\nContent-Type: text/markdown\r\n\r\n# Kafka\n\nKafka transactions.\r\n"
        + "--" + boundary + "--\r\n";
    var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/ingestions/previews/upload"))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    var report = json.readTree(response.body());
    assertEquals("COMPLETE", report.path("similarity").path("status").asText());
    assertFalse(report.path("similarity").path("matches").isEmpty());
    assertTrue(report.path("similarity").path("matches").toString().contains("LEXICAL"));
    assertTrue(report.path("similarity").path("matches").toString().contains("SEMANTIC"));
    assertEquals(1, count("SELECT count(*) FROM job"));
    assertFalse(Files.exists(documents.resolve("uploaded.md")));
    assertEquals(200, post("/api/v1/ingestions/previews/" + report.path("id").asText() + "/confirm", Map.of()).statusCode());
    assertEquals(2, count("SELECT count(*) FROM job"));
  }

  @Test void concurrentConfirmationsCreateOnlyOneJob() throws Exception {
    Files.writeString(documents.resolve("concurrent.txt"), "Concurrent confirmation example.");
    var preview = post("/api/v1/ingestions/previews/source", request("concurrent.txt"));
    assertEquals(200, preview.statusCode(), preview.body());
    String id = json.readTree(preview.body()).path("id").asText();
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/ingestions/previews/" + id + "/confirm"))
        .POST(HttpRequest.BodyPublishers.noBody()).build();
    var first = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var second = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var left = first.get();
    var right = second.get();
    assertEquals(200, left.statusCode(), left.body());
    assertEquals(200, right.statusCode(), right.body());
    assertEquals(json.readTree(left.body()).path("jobId"), json.readTree(right.body()).path("jobId"));
    assertEquals(1, count("SELECT count(*) FROM job"));
    assertEquals("COMPLETED", scalar("SELECT status FROM job"));
  }

  @Test void unreadableSourceIsReportedWithoutStartingAJob() throws Exception {
    var missing = post("/api/v1/ingestions/previews/source", request("does-not-exist.txt"));
    assertEquals(422, missing.statusCode(), missing.body());
    Files.writeString(documents.resolve("unknown.xyz"), "Content with unsupported extension.");
    assertEquals(422, post("/api/v1/ingestions/previews/source", request("unknown.xyz")).statusCode());
    assertEquals(0, count("SELECT count(*) FROM job"));
    assertEquals(0, embeddings.calls);
  }

  @Test void contractLimitsRejectBeforeAnyJobOrModelCall() throws Exception {
    Files.writeString(documents.resolve("sections.md"), "# Small\n\ntext\n\n".repeat(2001));
    assertEquals(422, post("/api/v1/ingestions/previews/source", request("sections.md")).statusCode());
    Files.writeString(documents.resolve("chunks.txt"), "part\n\n".repeat(10001));
    assertEquals(422, post("/api/v1/ingestions/previews/source", request("chunks.txt")).statusCode());
    Files.write(documents.resolve("large.txt"), new byte[10485761]);
    assertEquals(422, post("/api/v1/ingestions/previews/source", request("large.txt")).statusCode());
    assertEquals(0, count("SELECT count(*) FROM job"));
    assertEquals(0, embeddings.calls);
  }

  private Map<String,Object> request(String file) {
    return Map.of("sourceId", "test-files", "externalDocumentId", file);
  }
  private HttpResponse<String> ingest(String name, String content) throws Exception {
    Files.writeString(documents.resolve(name), content);
    return post("/api/v1/ingestions/source", request(name));
  }
  private HttpResponse<String> post(String path, Object body) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
        HttpResponse.BodyHandlers.ofString());
  }
  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
  private String scalar(String sql) { return db.sql(sql).query(String.class).single(); }
  private int count(String sql) { return db.sql(sql).query(Integer.class).single(); }

  @TestConfiguration static class Models {
    @Bean @Primary TestEmbeddings testEmbeddings() { return new TestEmbeddings(); }
  }
  static class TestEmbeddings implements EmbeddingProvider {
    volatile boolean fail;
    volatile String revision = "test-v1";
    int calls;
    public String id() { return "test"; }
    public String model() { return "deterministic"; }
    public String revision() { return revision; }
    public int dimensions() { return 1024; }
    public List<float[]> embed(List<String> texts) {
      if (fail) throw new IllegalStateException("Simulated embedding outage");
      calls += texts.size();
      return texts.stream().map(text -> { var vector = new float[1024]; vector[0] = 1; return vector; }).toList();
    }
  }
}
