package com.ansaradd.workflowragsrc.preflight.controller;

import com.ansaradd.workflowragapi.controller.IngestionPreviewController;
import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import com.ansaradd.workflowragsrc.preflight.config.PreflightProperties;
import com.ansaradd.workflowragsrc.preflight.exception.DocumentContractException;
import com.ansaradd.workflowragsrc.preflight.service.IngestionPreviewService;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionResponseMapper;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController

public class IngestionPreviewControllerImpl implements IngestionPreviewController {
  private final IngestionPreviewService service;
  private final SourceIngestionResponseMapper mapper;
  private final PreflightProperties properties;

  public IngestionPreviewControllerImpl(
      IngestionPreviewService service,
      SourceIngestionResponseMapper mapper,
      PreflightProperties properties
  ) {
    this.service = service;
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  public IngestionPreviewResponse previewSource(@RequestBody SourceIngestionRequest request) {
    return service.previewSource(request);
  }

  @Override
  public IngestionPreviewResponse previewUpload(
      @RequestParam String sourceId,
      @RequestParam String externalDocumentId,
      @RequestParam MultipartFile file
  ) throws IOException {
    if (file.getSize() > properties.maxBytes()) {
      throw new DocumentContractException("Document exceeds upload size limit");
    }
    try (var input = file.getInputStream()) {
      return service.previewUpload(sourceId, externalDocumentId, input.readNBytes(properties.maxBytes() + 1));
    }
  }

  @Override
  public IngestionPreviewResponse find(@PathVariable UUID id) {
    return service.find(id);
  }

  @Override
  public SourceIngestionResponse confirm(@PathVariable UUID id) {
    return mapper.map(service.confirm(id));
  }
}
