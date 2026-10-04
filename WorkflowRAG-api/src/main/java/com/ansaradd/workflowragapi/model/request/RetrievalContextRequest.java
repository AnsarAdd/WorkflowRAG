package com.ansaradd.workflowragapi.model.request;

import com.ansaradd.workflowragapi.model.enums.ContextExpansionStrategy;
import com.ansaradd.workflowragapi.model.enums.RetrievalProfile;

public record RetrievalContextRequest(
    String query,
    Integer limit,
    String sourceId,
    RetrievalProfile profile,
    ContextExpansionStrategy expansionStrategy
) {
}