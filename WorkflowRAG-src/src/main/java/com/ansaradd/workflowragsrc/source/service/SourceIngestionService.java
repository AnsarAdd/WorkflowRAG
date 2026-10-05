package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;

public interface SourceIngestionService {

  SourceIngestionResult ingest(SourceIngestionRequest request);
}