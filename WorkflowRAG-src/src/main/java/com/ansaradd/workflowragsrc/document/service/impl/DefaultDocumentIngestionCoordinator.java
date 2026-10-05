package com.ansaradd.workflowragsrc.document.service.impl;

import com.ansaradd.workflowragsrc.document.exception.VersionResolutionRequiredException;
import com.ansaradd.workflowragsrc.document.model.DocumentChangeStatus;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionBuildDecision;
import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.document.repository.DocumentRepository;
import com.ansaradd.workflowragsrc.document.service.DocumentChangeDetector;
import com.ansaradd.workflowragsrc.document.service.DocumentIngestionCoordinator;
import com.ansaradd.workflowragsrc.document.service.DocumentVersionBuildPolicy;
import com.ansaradd.workflowragsrc.document.service.DocumentVersionLifecycleService;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentIngestionCoordinator
    implements DocumentIngestionCoordinator {

  private final DocumentRepository documentRepository;
  private final DocumentChangeDetector documentChangeDetector;
  private final DocumentVersionBuildPolicy documentVersionBuildPolicy;
  private final DocumentVersionLifecycleService documentVersionLifecycleService;

  public DefaultDocumentIngestionCoordinator(
      DocumentRepository documentRepository,
      DocumentChangeDetector documentChangeDetector,
      DocumentVersionBuildPolicy documentVersionBuildPolicy,
      DocumentVersionLifecycleService documentVersionLifecycleService
  ) {
    this.documentRepository = documentRepository;
    this.documentChangeDetector = documentChangeDetector;
    this.documentVersionBuildPolicy = documentVersionBuildPolicy;
    this.documentVersionLifecycleService = documentVersionLifecycleService;
  }

  @Override
  public Optional<StoredDocumentVersion> process(
      PreparedDocument incomingDocument,
      SourceUpdatePolicy updatePolicy,
      boolean forceReindex
  ) {
    Objects.requireNonNull(
        incomingDocument,
        "incomingDocument must not be null"
    );
    Objects.requireNonNull(
        updatePolicy,
        "updatePolicy must not be null"
    );

    StoredDocument document =
        documentRepository.getOrCreate(
            incomingDocument.documentKey()
        );

    DocumentChangeStatus changeStatus =
        documentChangeDetector.detect(
            document,
            incomingDocument
        );

    DocumentVersionBuildDecision decision =
        forceReindex && changeStatus == DocumentChangeStatus.UNCHANGED
            ? DocumentVersionBuildDecision.BUILD
            : documentVersionBuildPolicy.decide(changeStatus, updatePolicy);

    return switch (decision) {
      case SKIP ->
          Optional.empty();

      case BUILD ->
          Optional.of(
              documentVersionLifecycleService.start(
                  document,
                  incomingDocument
              )
          );

      case REQUIRE_VERSION_RESOLUTION ->
          throw new VersionResolutionRequiredException(
              incomingDocument.documentKey()
          );
    };
  }
}