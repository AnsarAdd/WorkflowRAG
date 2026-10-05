package com.ansaradd.workflowragsrc.preflight.service;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;
import java.util.UUID;

public interface IngestionPreviewService {
  IngestionPreviewResponse previewSource(SourceIngestionRequest request);
  IngestionPreviewResponse previewUpload(String sourceId, String externalDocumentId, byte[] content);
  IngestionPreviewResponse find(UUID id);
  SourceIngestionResult confirm(UUID id);
}
