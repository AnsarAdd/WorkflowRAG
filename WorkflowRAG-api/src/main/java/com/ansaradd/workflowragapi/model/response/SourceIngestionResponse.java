package com.ansaradd.workflowragapi.model.response;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;

public record SourceIngestionResponse(
    String sourceId,
    String externalDocumentId,
    DocumentFormat format,
    String contentHash,
    java.util.UUID jobId,
    String status
) {
}