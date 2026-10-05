package com.ansaradd.workflowragsrc.document.repository;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragsrc.document.model.ActiveDocumentVersion;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionContent;
import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository {

  Optional<ActiveDocumentVersion> findActive(UUID documentId);

  DocumentVersionContent getContent(UUID versionId);

  StoredDocumentVersion createNextBuildingVersion(
      UUID documentId,
      DocumentFormat format,
      String contentHash,
      byte[] content
  );

  void activate(UUID versionId);

  void markFailed(UUID versionId);

  void resume(UUID versionId);
}