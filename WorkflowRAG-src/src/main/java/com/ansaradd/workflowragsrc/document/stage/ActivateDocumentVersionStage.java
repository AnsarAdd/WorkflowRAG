package com.ansaradd.workflowragsrc.document.stage;

import com.ansaradd.workflowragsrc.document.service.DocumentVersionLifecycleService;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ActivateDocumentVersionStage
    implements WorkflowStage {

  private final DocumentVersionLifecycleService lifecycleService;

  public ActivateDocumentVersionStage(
      DocumentVersionLifecycleService lifecycleService
  ) {
    this.lifecycleService = lifecycleService;
  }

  @Override
  public String id() {
    return "activate";
  }

  @Override
  public void execute(Job job) {
    Objects.requireNonNull(
        job,
        "job must not be null"
    );

    lifecycleService.complete(
        job.documentVersionId()
    );
  }
}