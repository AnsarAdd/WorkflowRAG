package com.ansaradd.workflowragsrc.ingestion.service;

import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import java.util.Optional;

public interface IngestionStartService {

  Optional<Job> start(
      Source source,
      PreparedDocument document,
      PipelineTemplate pipeline
  );
}