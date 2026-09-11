package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragsrc.source.model.Source;

public interface SourceRegistry {

  Source getRequired(String sourceId);
}