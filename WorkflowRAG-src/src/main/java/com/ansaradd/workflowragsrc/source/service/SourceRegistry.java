package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragsrc.source.model.Source;
import java.util.List;

public interface SourceRegistry {

  Source getRequired(String sourceId);

  List<Source> findAll();
}