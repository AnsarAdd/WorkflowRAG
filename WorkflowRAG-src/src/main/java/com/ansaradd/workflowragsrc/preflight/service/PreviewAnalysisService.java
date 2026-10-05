package com.ansaradd.workflowragsrc.preflight.service;

import com.ansaradd.workflowragapi.model.response.PreviewChangeSummary;
import com.ansaradd.workflowragapi.model.response.PreviewSimilarityReport;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import java.util.UUID;

public interface PreviewAnalysisService {
  PreviewChangeSummary changes(ParsedDocument document, UUID baselineVersionId);
  PreviewSimilarityReport similarity(ParsedDocument document, DocumentKey key);
}
