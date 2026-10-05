package com.ansaradd.workflowragapi.model.response;

import java.util.List;

public record PreviewSimilarityReport(
    String status,
    int sampledParts,
    List<PreviewSimilarityMatch> matches,
    List<String> warnings
) {
}
