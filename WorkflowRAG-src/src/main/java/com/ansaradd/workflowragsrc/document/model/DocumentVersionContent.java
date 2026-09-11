package com.ansaradd.workflowragsrc.document.model;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import java.util.UUID;

public record DocumentVersionContent(
    UUID id,
    UUID documentId,
    DocumentFormat format,
    String contentHash,
    byte[] content,
    DocumentVersionStatus status
) {
}