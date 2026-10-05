package com.ansaradd.workflowragapi.model.response;

public record PreviewChangeSummary(
    int previousParts,
    int incomingParts,
    int unchangedParts,
    int addedParts,
    int removedParts
) {
}
