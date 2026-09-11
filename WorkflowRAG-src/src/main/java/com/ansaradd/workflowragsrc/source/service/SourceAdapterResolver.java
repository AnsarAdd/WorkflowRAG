package com.ansaradd.workflowragsrc.source.service;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import com.ansaradd.workflowragsrc.source.adapter.SourceAdapter;

public interface SourceAdapterResolver {

  SourceAdapter resolve(SourceType sourceType);
}