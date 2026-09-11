package com.ansaradd.workflowragapi.model.response;

import java.util.List;

public record RetrievalSearchResponse(
    List<RetrievalHitResponse> hits
) {

  public RetrievalSearchResponse {
    hits = List.copyOf(hits);
  }
}