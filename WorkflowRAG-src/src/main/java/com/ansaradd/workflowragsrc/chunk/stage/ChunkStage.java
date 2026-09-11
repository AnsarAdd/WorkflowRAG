package com.ansaradd.workflowragsrc.chunk.stage;

import com.ansaradd.workflowragsrc.chunk.config.ChunkingProperties;
import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.chunk.repository.ChunkRepository;
import com.ansaradd.workflowragsrc.chunk.service.TextChunker;
import com.ansaradd.workflowragsrc.common.StableUuid;
import com.ansaradd.workflowragsrc.document.exception.DocumentSectionsNotFoundException;
import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
import com.ansaradd.workflowragsrc.source.service.ContentHashService;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.stage.IdempotentWorkflowStage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ChunkStage implements IdempotentWorkflowStage {

  private static final String PROCESSOR_VERSION =
      "paragraph-chunker-v1";

  private final SectionRepository sectionRepository;
  private final ChunkRepository chunkRepository;
  private final TextChunker textChunker;
  private final ChunkingProperties chunkingProperties;
  private final ContentHashService contentHashService;

  public ChunkStage(
      SectionRepository sectionRepository,
      ChunkRepository chunkRepository,
      TextChunker textChunker,
      ChunkingProperties chunkingProperties,
      ContentHashService contentHashService
  ) {
    this.sectionRepository = sectionRepository;
    this.chunkRepository = chunkRepository;
    this.textChunker = textChunker;
    this.chunkingProperties = chunkingProperties;
    this.contentHashService = contentHashService;
  }

  @Override
  public String id() {
    return "chunk";
  }

  @Override
  public String fingerprint(Job job) {
    Objects.requireNonNull(job, "job must not be null");

    List<Section> sections =
        getRequiredSections(
            job.documentVersionId()
        );

    return calculateFingerprint(sections);
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

    return chunkRepository.hasReusableChunking(
        job.documentVersionId(),
        fingerprint
    );
  }

  @Override
  public void execute(Job job) {
    Objects.requireNonNull(job, "job must not be null");

    List<Section> sections =
        getRequiredSections(
            job.documentVersionId()
        );

    List<Chunk> chunks =
        createChunks(sections);

    if (chunks.isEmpty()) {
      throw new IllegalStateException(
          "Chunking produced no chunks for document version: "
              + job.documentVersionId()
      );
    }

    chunkRepository.replaceForVersion(
        job.documentVersionId(),
        calculateFingerprint(sections),
        chunks
    );
  }

  private List<Chunk> createChunks(
      List<Section> sections
  ) {
    List<Chunk> chunks =
        new ArrayList<>();

    for (Section section : sections) {
      if (section.content().isBlank()) {
        continue;
      }

      List<String> contents =
          textChunker.chunk(
              section.content()
          );

      boolean splitSection =
          contents.size() > 1;

      for (int index = 0;
          index < contents.size();
          index++) {

        String content =
            contents.get(index);

        chunks.add(
            new Chunk(
                StableUuid.chunk(
                    section.id(),
                    index
                ),
                section.id(),
                index,
                content,
                contentHashService.calculate(
                    content.getBytes(
                        StandardCharsets.UTF_8
                    )
                ),
                splitSection
            )
        );
      }
    }

    return List.copyOf(chunks);
  }

  private List<Section> getRequiredSections(
      UUID documentVersionId
  ) {
    List<Section> sections =
        sectionRepository.findByDocumentVersionId(
            documentVersionId
        );

    if (sections.isEmpty()) {
      throw new DocumentSectionsNotFoundException(
          documentVersionId
      );
    }

    return sections;
  }

  private String calculateFingerprint(
      List<Section> sections
  ) {
    StringBuilder source =
        new StringBuilder();

    source
        .append(PROCESSOR_VERSION)
        .append('\n')
        .append("maxCharacters=")
        .append(
            chunkingProperties.maxCharacters()
        );

    for (Section section : sections) {
      source
          .append('\n')
          .append("section=")
          .append(section.stableKey())
          .append(':')
          .append(section.contentHash());
    }

    return contentHashService.calculate(
        source.toString()
            .getBytes(StandardCharsets.UTF_8)
    );
  }
}