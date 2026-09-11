package com.ansaradd.workflowragapi.model.response;

import java.time.Instant;

public record ApiErrorResponse(
    String code,
    String message,
    String path,
    Instant timestamp
) {
}