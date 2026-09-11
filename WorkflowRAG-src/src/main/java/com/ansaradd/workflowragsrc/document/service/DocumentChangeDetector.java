package com.ansaradd.workflowragsrc.document.service;

import com.ansaradd.workflowragsrc.document.model.DocumentChangeStatus;
import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public interface DocumentChangeDetector {

  DocumentChangeStatus detect(
      StoredDocument document,
      PreparedDocument incomingDocument
  );
}