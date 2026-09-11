package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.service.ContentHashService;
import com.ansaradd.workflowragsrc.source.service.DocumentPreparationService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentPreparationService
    implements DocumentPreparationService {

  private final ContentHashService contentHashService;

  public DefaultDocumentPreparationService(
      ContentHashService contentHashService
  ) {
    this.contentHashService = contentHashService;
  }

  @Override
  public PreparedDocument prepare(LoadedDocument document) {
    Objects.requireNonNull(document, "document must not be null");

    byte[] content = document.content();

    String contentHash =
        contentHashService.calculate(content);

    return new PreparedDocument(
        document.documentKey(),
        document.format(),
        content,
        contentHash,
        document.metadata()
    );
  }
}