package com.ansaradd.workflowragsrc.document.model;

import java.util.UUID;

public record Section(
    UUID id,
    UUID documentVersionId,
    String stableKey,
    UUID parentId,
    int sectionOrder,
    String title,
    int level,
    String content,
    String contentHash
) {
}