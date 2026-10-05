package com.ansaradd.workflowragsrc.preflight.service.impl;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragapi.model.response.PreviewSimilarityReport;
import com.ansaradd.workflowragsrc.document.repository.DocumentRepository;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.preflight.config.PreflightProperties;
import com.ansaradd.workflowragsrc.preflight.exception.DocumentContractException;
import com.ansaradd.workflowragsrc.preflight.exception.PreviewStateException;
import com.ansaradd.workflowragsrc.preflight.model.PreviewSnapshot;
import com.ansaradd.workflowragsrc.preflight.repository.PreviewRepository;
import com.ansaradd.workflowragsrc.preflight.service.DocumentContractValidator;
import com.ansaradd.workflowragsrc.preflight.service.IngestionPreviewService;
import com.ansaradd.workflowragsrc.preflight.service.PreviewAnalysisService;
import com.ansaradd.workflowragsrc.preflight.service.PreviewConfirmationService;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.model.SourceIngestionResult;
import com.ansaradd.workflowragsrc.source.model.SourceUpdatePolicy;
import com.ansaradd.workflowragsrc.source.service.DocumentPreparationService;
import com.ansaradd.workflowragsrc.source.service.SourceDocumentLoader;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import com.ansaradd.workflowragsrc.workflow.repository.JobRepository;
import com.ansaradd.workflowragsrc.workflow.service.WorkflowExecutor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultIngestionPreviewService implements IngestionPreviewService {
  private final SourceRegistry sources;
  private final SourceDocumentLoader loader;
  private final DocumentPreparationService preparation;
  private final DocumentContractValidator validator;
  private final DocumentRepository documents;
  private final DocumentVersionRepository versions;
  private final PreviewAnalysisService analysis;
  private final PreviewRepository previews;
  private final PreviewConfirmationService confirmation;
  private final WorkflowExecutor executor;
  private final JobRepository jobs;
  private final PreflightProperties properties;

  public DefaultIngestionPreviewService(
      SourceRegistry sources,
      SourceDocumentLoader loader,
      DocumentPreparationService preparation,
      DocumentContractValidator validator,
      DocumentRepository documents,
      DocumentVersionRepository versions,
      PreviewAnalysisService analysis,
      PreviewRepository previews,
      PreviewConfirmationService confirmation,
      WorkflowExecutor executor,
      JobRepository jobs,
      PreflightProperties properties
  ) {
    this.sources = sources;
    this.loader = loader;
    this.preparation = preparation;
    this.validator = validator;
    this.documents = documents;
    this.versions = versions;
    this.analysis = analysis;
    this.previews = previews;
    this.confirmation = confirmation;
    this.executor = executor;
    this.jobs = jobs;
    this.properties = properties;
  }

  @Override
  public IngestionPreviewResponse previewSource(SourceIngestionRequest request) {
    var source = sources.getRequired(request.sourceId());
    String force = request.parameters().getOrDefault("forceReindex", "false");
    if (!force.equalsIgnoreCase("true") && !force.equalsIgnoreCase("false")) {
      throw new IllegalArgumentException("forceReindex must be true or false");
    }
    var loaded = loader.load(source, request.externalDocumentId(), request.parameters());
    return preview(source, preparation.prepare(loaded), Boolean.parseBoolean(force));
  }

  @Override
  public IngestionPreviewResponse previewUpload(String sourceId, String externalDocumentId, byte[] content) {
    var source = sources.getRequired(sourceId);
    if (externalDocumentId == null || externalDocumentId.isBlank()) {
      throw new IllegalArgumentException("externalDocumentId must not be blank");
    }
    String name = externalDocumentId.replace('\\', '/');
    if (name.startsWith("/") || name.contains(":") || name.contains("\0")
        || java.util.Arrays.stream(name.split("/", -1))
            .anyMatch(part -> part.isBlank() || part.equals(".") || part.equals(".."))) {
      throw new IllegalArgumentException("externalDocumentId must be a canonical relative document path");
    }
    String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    DocumentFormat format = switch (extension) {
      case "txt" -> DocumentFormat.TEXT;
      case "md", "markdown" -> DocumentFormat.MARKDOWN;
      default -> throw new DocumentContractException("Only TXT and Markdown documents are supported");
    };
    return preview(source, preparation.prepare(new LoadedDocument(new DocumentKey(sourceId, name),
        format, content, Map.of())), false);
  }

  private IngestionPreviewResponse preview(Source source, PreparedDocument document, boolean force) {
    var parsed = validator.validate(document);
    var active = documents.findByKey(document.documentKey())
        .flatMap(existing -> versions.findActive(existing.id()));
    UUID baseline = active.map(version -> version.id()).orElse(null);
    boolean unchanged = active.map(version -> version.contentHash().equals(document.contentHash())).orElse(false);
    String status = unchanged && !force ? "UNCHANGED" : "READY";
    if (!unchanged && active.isPresent() && source.updatePolicy() == SourceUpdatePolicy.REQUIRE_VERSION_RESOLUTION) {
      status = "REQUIRES_VERSION_RESOLUTION";
    }
    var changes = analysis.changes(parsed, baseline);
    var similarity = status.equals("UNCHANGED")
        ? new PreviewSimilarityReport("NOT_RUN_UNCHANGED", 0, List.of(), List.of())
        : analysis.similarity(parsed, document.documentKey());
    var report = new IngestionPreviewResponse(UUID.randomUUID(), source.id(),
        document.documentKey().externalDocumentId(), document.contentHash(), baseline, status,
        status.equals("READY"), Instant.now().plus(properties.ttlHours(), ChronoUnit.HOURS),
        changes, similarity, null);
    previews.deleteExpired();
    previews.save(new PreviewSnapshot(document, force, report));
    return report;
  }

  @Override
  public IngestionPreviewResponse find(UUID id) {
    var report = previews.find(id, false).orElseThrow(() ->
        new PreviewStateException("PREVIEW_NOT_FOUND", "Preview not found")).report();
    return report.jobId() == null && !report.expiresAt().isAfter(Instant.now())
        ? report.withState("EXPIRED", false, null) : report;
  }

  @Override
  public SourceIngestionResult confirm(UUID id) {
    var started = confirmation.start(id);
    var result = started.result();
    if (!started.execute()) {
      return result;
    }
    executor.execute(result.job().id());
    return new SourceIngestionResult(result.document(), jobs.findById(result.job().id()).orElseThrow());
  }
}
