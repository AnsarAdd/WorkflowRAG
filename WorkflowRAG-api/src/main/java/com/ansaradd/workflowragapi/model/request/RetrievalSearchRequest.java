package com.ansaradd.workflowragapi.model.request;

public record RetrievalSearchRequest(
    String query,
    Integer limit,
    String sourceId
) {
}