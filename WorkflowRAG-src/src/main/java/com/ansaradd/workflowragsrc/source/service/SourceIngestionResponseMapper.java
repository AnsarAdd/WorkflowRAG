package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public interface SourceIngestionResponseMapper {

  SourceIngestionResponse map(PreparedDocument document);
}