package com.ansaradd.workflowragsrc.ingestion.service.impl;

import com.ansaradd.workflowragsrc.document.model.StoredDocumentVersion;
import com.ansaradd.workflowragsrc.document.service.DocumentIngestionCoordinator;
import com.ansaradd.workflowragsrc.ingestion.service.IngestionStartService;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.model.JobType;
import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import com.ansaradd.workflowragsrc.workflow.service.JobLifecycleService;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultIngestionStartService
    implements IngestionStartService {

  private final DocumentIngestionCoordinator documentIngestionCoordinator;
  private final JobLifecycleService jobLifecycleService;

  public DefaultIngestionStartService(
      DocumentIngestionCoordinator documentIngestionCoordinator,
      JobLifecycleService jobLifecycleService
  ) {
    this.documentIngestionCoordinator = documentIngestionCoordinator;
    this.jobLifecycleService = jobLifecycleService;
  }

  @Override
  @Transactional
  public Optional<Job> start(
      Source source,
      PreparedDocument document,
      PipelineTemplate pipeline,
      boolean forceReindex
  ) {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(pipeline, "pipeline must not be null");

    Optional<StoredDocumentVersion> version =
        documentIngestionCoordinator.process(
            document,
            source.updatePolicy(),
            forceReindex
        );

    if (version.isEmpty()) {
      return Optional.empty();
    }

    StoredDocumentVersion buildingVersion =
        version.get();

    Job job = jobLifecycleService.create(
        JobType.INGESTION,
        pipeline,
        source.id(),
        buildingVersion.documentId(),
        buildingVersion.id()
    );

    return Optional.of(job);
  }
}