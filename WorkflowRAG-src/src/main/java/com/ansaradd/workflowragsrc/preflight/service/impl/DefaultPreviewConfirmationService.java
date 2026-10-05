package com.ansaradd.workflowragsrc.preflight.service.impl;

import com.ansaradd.workflowragsrc.document.repository.DocumentRepository;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.ingestion.service.IngestionStartService;
import com.ansaradd.workflowragsrc.preflight.exception.PreviewStateException;
import com.ansaradd.workflowragsrc.preflight.model.PreviewConfirmation;
import com.ansaradd.workflowragsrc.preflight.repository.PreviewRepository;
import com.ansaradd.workflowragsrc.preflight.service.PreviewConfirmationService;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.service.PipelineRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPreviewConfirmationService implements PreviewConfirmationService {
  private final PreviewRepository previews;
  private final DocumentRepository documents;
  private final DocumentVersionRepository versions;
  private final SourceRegistry sources;
  private final PipelineRegistry pipelines;
  private final IngestionStartService ingestion;
  private final JobRepository jobs;

  public DefaultPreviewConfirmationService(
      PreviewRepository previews,
      DocumentRepository documents,
      DocumentVersionRepository versions,
      SourceRegistry sources,
      PipelineRegistry pipelines,
      IngestionStartService ingestion,
      JobRepository jobs
  ) {
    this.previews = previews;
    this.documents = documents;
    this.versions = versions;
    this.sources = sources;
    this.pipelines = pipelines;
    this.ingestion = ingestion;
    this.jobs = jobs;
  }

  @Override
  @Transactional
  public PreviewConfirmation start(UUID id) {
    var snapshot = previews.find(id, true).orElseThrow(() ->
        new PreviewStateException("PREVIEW_NOT_FOUND", "Preview not found"));
    var report = snapshot.report();
    if (report.jobId() != null) {
      return new PreviewConfirmation(new SourceIngestionResult(snapshot.document(),
          jobs.findById(report.jobId()).orElseThrow()), false);
    }
    if (!report.expiresAt().isAfter(Instant.now())) {
      throw new PreviewStateException("PREVIEW_EXPIRED", "Preview expired; create a new preview");
    }
    var document = documents.getOrCreate(snapshot.document().documentKey());
    previews.lockDocument(document.id());
    UUID active = versions.findActive(document.id()).map(version -> version.id()).orElse(null);
    if (!Objects.equals(active, report.baselineVersionId())) {
      throw new PreviewStateException("PREVIEW_STALE", "Active version changed; create a new preview");
    }
    if (report.status().equals("UNCHANGED")) {
      return new PreviewConfirmation(new SourceIngestionResult(snapshot.document(), null), false);
    }
    if (!report.canConfirm()) {
      throw new PreviewStateException("PREVIEW_BLOCKED", "Source requires explicit version resolution");
    }
    var source = sources.getRequired(report.sourceId());
    var job = ingestion.start(source, snapshot.document(), pipelines.getRequired(source.pipelineId()),
        snapshot.forceReindex()).orElseThrow(() ->
            new PreviewStateException("PREVIEW_STALE", "Document state changed; create a new preview"));
    previews.confirm(id, job.id());
    return new PreviewConfirmation(new SourceIngestionResult(snapshot.document(), job), true);
  }
}
