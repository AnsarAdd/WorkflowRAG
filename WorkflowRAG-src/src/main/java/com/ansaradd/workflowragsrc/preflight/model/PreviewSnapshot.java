package com.ansaradd.workflowragsrc.preflight.model;

import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;

public record PreviewSnapshot(
    PreparedDocument document,
    boolean forceReindex,
    IngestionPreviewResponse report
) {
}
