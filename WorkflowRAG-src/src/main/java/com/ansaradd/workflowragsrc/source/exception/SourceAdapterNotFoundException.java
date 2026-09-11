package com.ansaradd.workflowragsrc.source.exception;

import com.ansaradd.workflowragapi.model.enums.SourceType;

public class SourceAdapterNotFoundException extends RuntimeException {

  public SourceAdapterNotFoundException(SourceType sourceType) {
    super("No SourceAdapter registered for source type: " + sourceType);
  }
}