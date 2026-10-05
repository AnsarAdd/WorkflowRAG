package com.ansaradd.workflowragsrc.preflight.model;

import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;

public record PreviewConfirmation(SourceIngestionResult result, boolean execute) {
}
