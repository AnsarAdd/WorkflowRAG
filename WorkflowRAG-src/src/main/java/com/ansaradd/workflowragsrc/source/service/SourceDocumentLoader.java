package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import java.util.Map;


public interface SourceDocumentLoader {

  LoadedDocument load(
      Source source,
      String externalDocumentId,
      Map<String, String> parameters
  );
}