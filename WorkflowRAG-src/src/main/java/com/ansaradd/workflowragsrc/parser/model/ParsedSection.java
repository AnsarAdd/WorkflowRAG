package com.ansaradd.workflowragsrc.parser.model;

public record ParsedSection(
    String stableKey,
    String parentStableKey,
    String title,
    int level,
    String content
) {
}