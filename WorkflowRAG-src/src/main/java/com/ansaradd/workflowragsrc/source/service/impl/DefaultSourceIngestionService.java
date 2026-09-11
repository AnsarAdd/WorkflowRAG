package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragsrc.document.service.DocumentIngestionCoordinator;
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
import org.springframework.stereotype.Service;

@Service
public class DefaultSourceIngestionService
    implements SourceIngestionService {

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
      IngestionStartService ingestionStartService
  ) {
    this.sourceRegistry = sourceRegistry;
    this.sourceDocumentLoader = sourceDocumentLoader;
    this.documentPreparationService = documentPreparationService;
    this.pipelineRegistry = pipelineRegistry;
    this.ingestionStartService = ingestionStartService;
  }

  @Override
  public PreparedDocument ingest(SourceIngestionRequest request) {
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

    PipelineTemplate pipeline =
        pipelineRegistry.getRequired(
            source.pipelineId()
        );

    ingestionStartService.start(
        source,
        preparedDocument,
        pipeline
    );

    return preparedDocument;
  }
}