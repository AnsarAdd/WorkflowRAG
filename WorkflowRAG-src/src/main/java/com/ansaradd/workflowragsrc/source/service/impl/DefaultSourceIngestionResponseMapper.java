package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.service.SourceIngestionResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultSourceIngestionResponseMapper
    implements SourceIngestionResponseMapper {

  @Override
  public SourceIngestionResponse map(PreparedDocument document) {
    return new SourceIngestionResponse(
        document.documentKey().sourceId(),
        document.documentKey().externalDocumentId(),
        document.format(),
        document.contentHash()
    );
  }
}