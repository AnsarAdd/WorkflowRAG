package com.ansaradd.workflowragsrc.document.service;

import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import java.util.Optional;

public interface DocumentIngestionCoordinator {

  Optional<StoredDocumentVersion> process(
      PreparedDocument incomingDocument,
      SourceUpdatePolicy updatePolicy
  );
}