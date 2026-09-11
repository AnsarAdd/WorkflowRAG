package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public interface DocumentPreparationService {

  PreparedDocument prepare(LoadedDocument document);
}