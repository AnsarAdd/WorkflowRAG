package com.ansaradd.workflowragsrc.source.model;

import com.ansaradd.workflowragsrc.workflow.model.Job;

/** A null job means the active document already has the same content. */
public record SourceIngestionResult(PreparedDocument document, Job job) {}