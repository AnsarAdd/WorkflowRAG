package com.ansaradd.workflowragsrc.preflight.service;

import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public interface DocumentContractValidator {
  ParsedDocument validate(PreparedDocument document);
}
