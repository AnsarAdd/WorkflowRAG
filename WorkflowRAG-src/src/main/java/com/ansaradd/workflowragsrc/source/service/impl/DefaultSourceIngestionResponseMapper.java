package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultSourceIngestionResponseMapper implements SourceIngestionResponseMapper {
  @Override
  public SourceIngestionResponse map(SourceIngestionResult result) {
    var document = result.document();
    var job = result.job();
    return new SourceIngestionResponse(
        document.documentKey().sourceId(), document.documentKey().externalDocumentId(),
        document.format(), document.contentHash(), job == null ? null : job.id(),
        job == null ? "UNCHANGED" : job.status().name());
  }
}