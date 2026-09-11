package com.ansaradd.workflowragsrc.source.adapter;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.SourceLoadContext;

public interface SourceAdapter {

  SourceType sourceType();

  LoadedDocument load(SourceLoadContext context);

}