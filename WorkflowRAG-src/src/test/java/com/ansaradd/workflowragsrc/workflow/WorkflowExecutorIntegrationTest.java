//package com.ansaradd.workflowragsrc.workflow;
//
//import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
//import com.ansaradd.workflowragsrc.document.model.StoredDocument;
//import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
//import com.ansaradd.workflowragsrc.document.repository.DocumentRepository;
//import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
//import com.ansaradd.workflowragsrc.source.model.DocumentKey;
//import com.ansaradd.workflowragsrc.workflow.exception.WorkflowExecutionException;
//import com.ansaradd.workflowragsrc.workflow.model.Job;
//import com.ansaradd.workflowragsrc.workflow.model.JobStatus;
//import com.ansaradd.workflowragsrc.workflow.model.JobStep;
//import com.ansaradd.workflowragsrc.workflow.model.JobStepStatus;
//import com.ansaradd.workflowragsrc.workflow.model.JobType;
//import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
//import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
//import com.ansaradd.workflowragsrc.workflow.repository.JobStepRepository;
//import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
//import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
//import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.postgresql.PostgreSQLContainer;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//@Testcontainers
//@SpringBootTest(
//    webEnvironment = SpringBootTest.WebEnvironment.NONE
//)
//class WorkflowExecutorIntegrationTest {
//
//  @Container
//  static final PostgreSQLContainer postgres =
//      new PostgreSQLContainer("pgvector/pgvector:pg17")
//          .withDatabaseName("workflowrag")
//          .withUsername("workflowrag")
//          .withPassword("workflowrag");
//
//  @DynamicPropertySource
//  static void datasourceProperties(
//      DynamicPropertyRegistry registry
//  ) {
//    registry.add(
//        "spring.datasource.url",
//        postgres::getJdbcUrl
//    );
//    registry.add(
//        "spring.datasource.username",
//        postgres::getUsername
//    );
//    registry.add(
//        "spring.datasource.password",
//        postgres::getPassword
//    );
//  }
//
//  @Autowired
//  private DocumentRepository documentRepository;
//
//  @Autowired
//  private DocumentVersionRepository documentVersionRepository;
//
//  @Autowired
//  private JobLifecycleService jobLifecycleService;
//
//  @Autowired
//  private JobRepository jobRepository;
//
//  @Autowired
//  private JobStepRepository jobStepRepository;
//
//  @Autowired
//  private WorkflowExecutor workflowExecutor;
//
//  @Autowired
//  private TestStageA stageA;
//
//  @Autowired
//  private TestStageB stageB;
//
//  @Autowired
//  private TestStageC stageC;
//
//  @Test
//  void shouldResumeFailedWorkflowFromFailedStep() {
//    String sourceId = "workflow-test";
//
//    StoredDocument document =
//        documentRepository.getOrCreate(
//            new DocumentKey(
//                sourceId,
//                "workflow-test.txt"
//            )
//        );
//
//    StoredDocumentVersion version =
//        documentVersionRepository.createNextBuildingVersion(
//            document.id(),
//            DocumentFormat.TEXT,
//            "a".repeat(64),
//            "workflow test"
//                .getBytes(StandardCharsets.UTF_8)
//        );
//
//    PipelineTemplate pipeline =
//        new PipelineTemplate(
//            "workflow-integration-test",
//            List.of(
//                "test-stage-a",
//                "test-stage-b",
//                "test-stage-c"
//            )
//        );
//
//    Job job =
//        jobLifecycleService.create(
//            JobType.INGESTION,
//            pipeline,
//            sourceId,
//            document.id(),
//            version.id()
//        );
//
//    assertThrows(
//        WorkflowExecutionException.class,
//        () -> workflowExecutor.execute(job.id())
//    );
//
//    assertFailedState(job.id());
//
//    assertEquals(1, stageA.calls());
//    assertEquals(1, stageB.calls());
//    assertEquals(0, stageC.calls());
//
//    stageB.allowSuccess();
//
//    workflowExecutor.execute(job.id());
//
//    assertCompletedState(job.id());
//
//    assertEquals(1, stageA.calls());
//    assertEquals(2, stageB.calls());
//    assertEquals(1, stageC.calls());
//  }
//
//  private void assertFailedState(UUID jobId) {
//    Job job = jobRepository.findById(jobId)
//        .orElseThrow();
//
//    assertEquals(
//        JobStatus.FAILED,
//        job.status()
//    );
//
//    assertEquals(
//        "test-stage-b",
//        job.currentStage()
//    );
//
//    List<JobStep> steps =
//        jobStepRepository.findByJobId(jobId);
//
//    assertEquals(3, steps.size());
//
//    assertStep(
//        steps.get(0),
//        "test-stage-a",
//        JobStepStatus.SUCCEEDED,
//        1
//    );
//
//    assertStep(
//        steps.get(1),
//        "test-stage-b",
//        JobStepStatus.FAILED,
//        1
//    );
//
//    assertStep(
//        steps.get(2),
//        "test-stage-c",
//        JobStepStatus.PENDING,
//        0
//    );
//  }
//
//  private void assertCompletedState(UUID jobId) {
//    Job job = jobRepository.findById(jobId)
//        .orElseThrow();
//
//    assertEquals(
//        JobStatus.COMPLETED,
//        job.status()
//    );
//
//    assertNull(job.currentStage());
//
//    List<JobStep> steps =
//        jobStepRepository.findByJobId(jobId);
//
//    assertEquals(3, steps.size());
//
//    assertStep(
//        steps.get(0),
//        "test-stage-a",
//        JobStepStatus.SUCCEEDED,
//        1
//    );
//
//    assertStep(
//        steps.get(1),
//        "test-stage-b",
//        JobStepStatus.SUCCEEDED,
//        2
//    );
//
//    assertStep(
//        steps.get(2),
//        "test-stage-c",
//        JobStepStatus.SUCCEEDED,
//        1
//    );
//  }
//
//  private void assertStep(
//      JobStep step,
//      String stage,
//      JobStepStatus status,
//      int attempt
//  ) {
//    assertEquals(stage, step.stage());
//    assertEquals(status, step.status());
//    assertEquals(attempt, step.attempt());
//  }
//
//  @TestConfiguration
//  static class StageConfiguration {
//
//    @Bean
//    TestStageA testStageA() {
//      return new TestStageA();
//    }
//
//    @Bean
//    TestStageB testStageB() {
//      return new TestStageB();
//    }
//
//    @Bean
//    TestStageC testStageC() {
//      return new TestStageC();
//    }
//  }
//
//  static class TestStageA implements WorkflowStage {
//
//    private final AtomicInteger calls =
//        new AtomicInteger();
//
//    @Override
//    public String id() {
//      return "test-stage-a";
//    }
//
//    @Override
//    public void execute(Job job) {
//      calls.incrementAndGet();
//    }
//
//    int calls() {
//      return calls.get();
//    }
//  }
//
//  static class TestStageB implements WorkflowStage {
//
//    private final AtomicInteger calls =
//        new AtomicInteger();
//
//    private final AtomicBoolean shouldFail =
//        new AtomicBoolean(true);
//
//    @Override
//    public String id() {
//      return "test-stage-b";
//    }
//
//    @Override
//    public void execute(Job job) {
//      calls.incrementAndGet();
//
//      if (shouldFail.get()) {
//        throw new IllegalStateException(
//            "Expected test failure"
//        );
//      }
//    }
//
//    void allowSuccess() {
//      shouldFail.set(false);
//    }
//
//    int calls() {
//      return calls.get();
//    }
//  }
//
//  static class TestStageC implements WorkflowStage {
//
//    private final AtomicInteger calls =
//        new AtomicInteger();
//
//    @Override
//    public String id() {
//      return "test-stage-c";
//    }
//
//    @Override
//    public void execute(Job job) {
//      calls.incrementAndGet();
//    }
//
//    int calls() {
//      return calls.get();
//    }
//  }
//}