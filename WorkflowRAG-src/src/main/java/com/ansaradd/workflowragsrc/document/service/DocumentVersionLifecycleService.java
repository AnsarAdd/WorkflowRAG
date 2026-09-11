package com.ansaradd.workflowragsrc.document.service;

import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import java.util.UUID;

public interface DocumentVersionLifecycleService {

  StoredDocumentVersion start(
      StoredDocument document,
      PreparedDocument incomingDocument
  );

  void complete(UUID versionId);

  void fail(UUID versionId);
}