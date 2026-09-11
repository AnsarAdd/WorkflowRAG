package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public interface SourceIngestionService {

  PreparedDocument ingest(SourceIngestionRequest request);
}