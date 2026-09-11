package com.ansaradd.workflowragsrc.retrieval.stage;

import com.ansaradd.workflowragsrc.retrieval.repository.RetrievalIndexRepository;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.stage.WorkflowStage;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RetrievalIndexStage
    implements WorkflowStage {

  private final RetrievalIndexRepository retrievalIndexRepository;

  public RetrievalIndexStage(
      RetrievalIndexRepository retrievalIndexRepository
  ) {
    this.retrievalIndexRepository =
        retrievalIndexRepository;
  }

  @Override
  public String id() {
    return "index";
  }

  @Override
  public void execute(Job job) {
    Objects.requireNonNull(
        job,
        "job must not be null"
    );

    retrievalIndexRepository.replaceForVersion(
        job.documentVersionId()
    );
  }
}