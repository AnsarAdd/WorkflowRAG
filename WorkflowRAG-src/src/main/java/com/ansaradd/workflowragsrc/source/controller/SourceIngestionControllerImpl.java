package com.ansaradd.workflowragsrc.source.controller;

import com.ansaradd.workflowragapi.controller.SourceIngestionController;
import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionResponseMapper;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SourceIngestionControllerImpl implements SourceIngestionController {

  private final SourceIngestionService sourceIngestionService;
  private final SourceIngestionResponseMapper responseMapper;

  public SourceIngestionControllerImpl(
      SourceIngestionService sourceIngestionService,
      SourceIngestionResponseMapper responseMapper
  ) {
    this.sourceIngestionService = sourceIngestionService;
    this.responseMapper = responseMapper;
  }

  @Override
  public ResponseEntity<SourceIngestionResponse> ingestSource(
      @RequestBody SourceIngestionRequest request
  ) {
    PreparedDocument preparedDocument =
        sourceIngestionService.ingest(request);

    return ResponseEntity.ok(
        responseMapper.map(preparedDocument)
    );
  }
}