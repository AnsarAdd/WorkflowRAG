package com.ansaradd.workflowragsrc.document.service.impl;

import com.ansaradd.workflowragsrc.document.model.ActiveDocumentVersion;
import com.ansaradd.workflowragsrc.document.model.DocumentChangeStatus;
import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.document.service.DocumentChangeDetector;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentChangeDetector
    implements DocumentChangeDetector {

  private final DocumentVersionRepository documentVersionRepository;

  public DefaultDocumentChangeDetector(
      DocumentVersionRepository documentVersionRepository
  ) {
    this.documentVersionRepository = documentVersionRepository;
  }

  @Override
  public DocumentChangeStatus detect(
      StoredDocument document,
      PreparedDocument incomingDocument
  ) {
    Objects.requireNonNull(
        document,
        "document must not be null"
    );

    Objects.requireNonNull(
        incomingDocument,
        "incomingDocument must not be null"
    );

    if (!document.documentKey()
        .equals(incomingDocument.documentKey())) {

      throw new IllegalArgumentException(
          "Stored document and incoming document have different document keys"
      );
    }

    Optional<ActiveDocumentVersion> activeVersion =
        documentVersionRepository.findActive(
            document.id()
        );

    if (activeVersion.isEmpty()) {
      return DocumentChangeStatus.NEW;
    }

    return activeVersion.get()
        .contentHash()
        .equals(incomingDocument.contentHash())
        ? DocumentChangeStatus.UNCHANGED
        : DocumentChangeStatus.CHANGED;
  }
}