package com.ansaradd.workflowragsrc.document.service;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragsrc.document.exception.UnsupportedDocumentFormatException;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;

public interface DocumentParsingService {

  ParsedDocument parse(
      DocumentFormat format,
      byte[] content
  ) throws UnsupportedDocumentFormatException;
}