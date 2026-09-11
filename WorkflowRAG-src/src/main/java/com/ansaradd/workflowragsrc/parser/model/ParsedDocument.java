package com.ansaradd.workflowragsrc.parser.model;

import java.util.List;

public record ParsedDocument(
    List<ParsedSection> sections
) {
}