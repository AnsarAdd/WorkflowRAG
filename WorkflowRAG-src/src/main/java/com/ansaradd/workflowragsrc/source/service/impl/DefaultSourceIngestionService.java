package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragsrc.preflight.service.DocumentContractValidator;
import com.ansaradd.workflowragsrc.ingestion.service.IngestionStartService;
import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.service.DocumentPreparationService;
import com.ansaradd.workflowragsrc.source.service.SourceDocumentLoader;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionService;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import com.ansaradd.workflowragsrc.workflow.service.PipelineRegistry;
import java.util.Objects;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import org.springframework.stereotype.Service;

@Service
public class DefaultSourceIngestionService
    implements SourceIngestionService {

  private final DocumentContractValidator contractValidator;
  private final WorkflowExecutor workflowExecutor;
  private final JobRepository jobRepository;
  private final SourceRegistry sourceRegistry;
  private final SourceDocumentLoader sourceDocumentLoader;
  private final DocumentPreparationService documentPreparationService;
  private final PipelineRegistry pipelineRegistry;
  private final IngestionStartService ingestionStartService;

  public DefaultSourceIngestionService(
      SourceRegistry sourceRegistry,
      SourceDocumentLoader sourceDocumentLoader,
      DocumentPreparationService documentPreparationService,
      PipelineRegistry pipelineRegistry,
      IngestionStartService ingestionStartService,
      WorkflowExecutor workflowExecutor,
      JobRepository jobRepository,
      DocumentContractValidator contractValidator
  ) {
    this.contractValidator = contractValidator;
    this.workflowExecutor = workflowExecutor;
    this.jobRepository = jobRepository;
    this.sourceRegistry = sourceRegistry;
    this.sourceDocumentLoader = sourceDocumentLoader;
    this.documentPreparationService = documentPreparationService;
    this.pipelineRegistry = pipelineRegistry;
    this.ingestionStartService = ingestionStartService;
  }

  @Override
  public SourceIngestionResult ingest(SourceIngestionRequest request) {
    Objects.requireNonNull(
        request,
        "request must not be null"
    );

    Source source =
        sourceRegistry.getRequired(
            request.sourceId()
        );

    LoadedDocument loadedDocument =
        sourceDocumentLoader.load(
            source,
            request.externalDocumentId(),
            request.parameters()
        );

    PreparedDocument preparedDocument =
        documentPreparationService.prepare(
            loadedDocument
        );

    contractValidator.validate(preparedDocument);

    PipelineTemplate pipeline =
        pipelineRegistry.getRequired(
            source.pipelineId()
        );

    String forceParameter = request.parameters().getOrDefault("forceReindex", "false");
    if (!forceParameter.equalsIgnoreCase("true") && !forceParameter.equalsIgnoreCase("false")) {
      throw new IllegalArgumentException("forceReindex must be true or false");
    }
    var job = ingestionStartService.start(
        source,
        preparedDocument,
        pipeline,
        Boolean.parseBoolean(forceParameter)
    );

    if (job.isEmpty()) {
      return new SourceIngestionResult(preparedDocument, null);
    }
    // start() commits version/job creation before any expensive stage executes.
    workflowExecutor.execute(job.get().id());
    return new SourceIngestionResult(
        preparedDocument, jobRepository.findById(job.get().id()).orElseThrow());
  }
}