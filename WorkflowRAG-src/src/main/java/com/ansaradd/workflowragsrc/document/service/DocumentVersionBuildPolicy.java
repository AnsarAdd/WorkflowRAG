package com.ansaradd.workflowragsrc.document.service;

import com.ansaradd.workflowragsrc.document.model.DocumentChangeStatus;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionBuildDecision;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;

public interface DocumentVersionBuildPolicy {

  DocumentVersionBuildDecision decide(
      DocumentChangeStatus changeStatus,
      SourceUpdatePolicy updatePolicy
  );
}