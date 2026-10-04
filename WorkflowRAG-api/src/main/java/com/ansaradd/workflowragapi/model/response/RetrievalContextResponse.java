package com.ansaradd.workflowragapi.model.response;

import java.util.List;

public record RetrievalContextResponse(
    List<ResolvedContextResponse> contexts
) {

  public RetrievalContextResponse {
    contexts = List.copyOf(contexts);
  }
}