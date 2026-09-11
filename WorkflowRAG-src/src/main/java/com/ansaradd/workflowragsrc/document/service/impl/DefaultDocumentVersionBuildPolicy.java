package com.ansaradd.workflowragsrc.document.service.impl;

import com.ansaradd.workflowragsrc.document.model.DocumentChangeStatus;
import com.ansaradd.workflowragsrc.document.service.DocumentVersionBuildPolicy;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionBuildDecision;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentVersionBuildPolicy
    implements DocumentVersionBuildPolicy {

  @Override
  public DocumentVersionBuildDecision decide(
      DocumentChangeStatus changeStatus,
      SourceUpdatePolicy updatePolicy
  ) {
    Objects.requireNonNull(changeStatus, "changeStatus must not be null");
    Objects.requireNonNull(updatePolicy, "updatePolicy must not be null");

    return switch (changeStatus) {
      case NEW -> DocumentVersionBuildDecision.BUILD;

      case UNCHANGED -> DocumentVersionBuildDecision.SKIP;

      case CHANGED -> switch (updatePolicy) {
        case AUTHORITATIVE_SNAPSHOT -> DocumentVersionBuildDecision.BUILD;

        case REQUIRE_VERSION_RESOLUTION -> DocumentVersionBuildDecision.REQUIRE_VERSION_RESOLUTION;
      };
    };
  }
}