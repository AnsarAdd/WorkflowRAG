package com.ansaradd.workflowragapi.model.request;

import com.ansaradd.workflowragapi.model.enums.RetrievalProfile;

public record RetrievalSearchRequest(
    String query,
    Integer limit,
    String sourceId,
    RetrievalProfile profile
) {
}