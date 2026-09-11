package com.ansaradd.workflowragsrc.document.service.impl;

import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.document.service.DocumentVersionLifecycleService;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentVersionLifecycleService
    implements DocumentVersionLifecycleService {

  private final DocumentVersionRepository documentVersionRepository;

  public DefaultDocumentVersionLifecycleService(
      DocumentVersionRepository documentVersionRepository
  ) {
    this.documentVersionRepository =
        documentVersionRepository;
  }

  @Override
  public StoredDocumentVersion start(
      StoredDocument document,
      PreparedDocument incomingDocument
  ) {
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(incomingDocument, "incomingDocument must not be null");

    if (!document.documentKey().equals(incomingDocument.documentKey())) {
      throw new IllegalArgumentException(
          "Stored document and incoming document have different document keys"
      );
    }

    byte[] content = incomingDocument.content();

    return documentVersionRepository.createNextBuildingVersion(
        document.id(),
        incomingDocument.format(),
        incomingDocument.contentHash(),
        content
    );
  }

  @Override
  @Transactional
  public void complete(
      UUID versionId
  ) {
    Objects.requireNonNull(
        versionId,
        "versionId must not be null"
    );

    documentVersionRepository.activate(
        versionId
    );
  }

  @Override
  @Transactional
  public void fail(
      UUID versionId
  ) {
    Objects.requireNonNull(
        versionId,
        "versionId must not be null"
    );

    documentVersionRepository.markFailed(
        versionId
    );
  }
}
