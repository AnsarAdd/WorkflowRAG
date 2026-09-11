package com.ansaradd.workflowragsrc.parser.stage;

import com.ansaradd.workflowragsrc.document.exception.InvalidDocumentVersionStateException;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionContent;
import com.ansaradd.workflowragsrc.document.model.DocumentVersionStatus;
import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.document.repository.DocumentVersionRepository;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
import com.ansaradd.workflowragsrc.document.service.DocumentParsingService;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.parser.model.ParsedSection;
import com.ansaradd.workflowragsrc.source.service.ContentHashService;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.stage.IdempotentWorkflowStage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParseStage implements IdempotentWorkflowStage {

  private static final String PROCESSOR_VERSION =
      "native-parser-v1";

  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentParsingService documentParsingService;
  private final SectionRepository sectionRepository;
  private final ContentHashService contentHashService;

  public ParseStage(
      DocumentVersionRepository documentVersionRepository,
      DocumentParsingService documentParsingService,
      SectionRepository sectionRepository,
      ContentHashService contentHashService
  ) {
    this.documentVersionRepository = documentVersionRepository;
    this.documentParsingService = documentParsingService;
    this.sectionRepository = sectionRepository;
    this.contentHashService = contentHashService;
  }

  @Override
  public String id() {
    return "parse";
  }

  @Override
  public String fingerprint(Job job) {
    Objects.requireNonNull(job, "job must not be null");

    DocumentVersionContent version =
        documentVersionRepository.getContent(
            job.documentVersionId()
        );

    return calculateFingerprint(version);
  }

  @Override
  public boolean canReuse(
      Job job,
      String fingerprint
  ) {
    Objects.requireNonNull(job, "job must not be null");
    Objects.requireNonNull(
        fingerprint,
        "fingerprint must not be null"
    );

    return sectionRepository.hasReusableParse(
        job.documentVersionId(),
        fingerprint
    );
  }

  @Override
  public void execute(Job job) {
    Objects.requireNonNull(job, "job must not be null");

    DocumentVersionContent version =
        documentVersionRepository.getContent(
            job.documentVersionId()
        );

    if (version.status() != DocumentVersionStatus.BUILDING) {
      throw new InvalidDocumentVersionStateException(
          version.id(),
          version.status(),
          "parse"
      );
    }

    ParsedDocument parsedDocument =
        documentParsingService.parse(
            version.format(),
            version.content()
        );

    Objects.requireNonNull(
        parsedDocument,
        "parsedDocument must not be null"
    );

    List<Section> sections =
        mapSections(
            version.id(),
            parsedDocument
        );

    if (sections.isEmpty()) {
      throw new IllegalStateException(
          "Parser produced no sections for document version: "
              + version.id()
      );
    }

    sectionRepository.replaceForVersion(
        version.id(),
        calculateFingerprint(version),
        sections
    );
  }

  private String calculateFingerprint(
      DocumentVersionContent version
  ) {
    String fingerprintSource =
        PROCESSOR_VERSION
            + "\nformat=" + version.format().name()
            + "\ncontentHash=" + version.contentHash();

    return contentHashService.calculate(
        fingerprintSource.getBytes(StandardCharsets.UTF_8)
    );
  }

  private List<Section> mapSections(
      UUID documentVersionId,
      ParsedDocument parsedDocument
  ) {
    List<ParsedSection> parsedSections =
        parsedDocument.sections();

    Objects.requireNonNull(
        parsedSections,
        "parsedDocument.sections must not be null"
    );

    validateStableKeys(parsedSections);

    Map<String, UUID> idsByStableKey =
        new HashMap<>();

    for (ParsedSection parsedSection : parsedSections) {
      idsByStableKey.put(
          parsedSection.stableKey(),
          UUID.randomUUID()
      );
    }

    List<Section> sections =
        new ArrayList<>(parsedSections.size());

    for (int sectionOrder = 0;
        sectionOrder < parsedSections.size();
        sectionOrder++) {

      ParsedSection parsedSection =
          parsedSections.get(sectionOrder);

      UUID parentId =
          resolveParentId(
              parsedSection,
              idsByStableKey
          );

      String content =
          parsedSection.content() == null
              ? ""
              : parsedSection.content();

      String contentHash =
          contentHashService.calculate(
              content.getBytes(StandardCharsets.UTF_8)
          );

      sections.add(
          new Section(
              idsByStableKey.get(
                  parsedSection.stableKey()
              ),
              documentVersionId,
              parsedSection.stableKey(),
              parentId,
              sectionOrder,
              parsedSection.title(),
              parsedSection.level(),
              content,
              contentHash
          )
      );
    }

    return List.copyOf(sections);
  }

  private UUID resolveParentId(
      ParsedSection section,
      Map<String, UUID> idsByStableKey
  ) {
    String parentStableKey =
        section.parentStableKey();

    if (parentStableKey == null) {
      return null;
    }

    UUID parentId =
        idsByStableKey.get(parentStableKey);

    if (parentId == null) {
      throw new IllegalStateException(
          "Parent section '"
              + parentStableKey
              + "' not found for section '"
              + section.stableKey()
              + "'"
      );
    }

    return parentId;
  }

  private void validateStableKeys(
      List<ParsedSection> sections
  ) {
    Set<String> stableKeys = new HashSet<>();

    for (ParsedSection section : sections) {
      Objects.requireNonNull(
          section,
          "parsed section must not be null"
      );

      String stableKey =
          section.stableKey();

      if (stableKey == null
          || stableKey.isBlank()) {
        throw new IllegalStateException(
            "Parsed section stableKey must not be null or blank"
        );
      }

      if (!stableKeys.add(stableKey)) {
        throw new IllegalStateException(
            "Duplicate parsed section stableKey: "
                + stableKey
        );
      }
    }
  }
}