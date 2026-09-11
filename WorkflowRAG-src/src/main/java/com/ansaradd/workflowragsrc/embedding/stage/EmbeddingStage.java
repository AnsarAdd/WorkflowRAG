package com.ansaradd.workflowragsrc.embedding.stage;

import com.ansaradd.workflowragsrc.chunk.exception.DocumentChunksNotFoundException;
import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.chunk.repository.ChunkRepository;
import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
import com.ansaradd.workflowragsrc.embedding.model.ChunkEmbedding;
import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
import com.ansaradd.workflowragsrc.embedding.repository.ChunkEmbeddingRepository;
import com.ansaradd.workflowragsrc.embedding.service.EmbeddingTextComposer;
import com.ansaradd.workflowragsrc.source.service.ContentHashService;
import com.ansaradd.workflowragsrc.workflow.model.Job;
import com.ansaradd.workflowragsrc.workflow.stage.IdempotentWorkflowStage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingStage implements IdempotentWorkflowStage {

  private final ChunkRepository chunkRepository;
  private final SectionRepository sectionRepository;
  private final ChunkEmbeddingRepository embeddingRepository;
  private final EmbeddingTextComposer textComposer;
  private final EmbeddingProvider embeddingProvider;
  private final ContentHashService contentHashService;

  public EmbeddingStage(
      ChunkRepository chunkRepository,
      SectionRepository sectionRepository,
      ChunkEmbeddingRepository embeddingRepository,
      EmbeddingTextComposer textComposer,
      EmbeddingProvider embeddingProvider,
      ContentHashService contentHashService
  ) {
    this.chunkRepository = chunkRepository;
    this.sectionRepository = sectionRepository;
    this.embeddingRepository = embeddingRepository;
    this.textComposer = textComposer;
    this.embeddingProvider = embeddingProvider;
    this.contentHashService = contentHashService;
  }

  private static final Logger log =
      LoggerFactory.getLogger(
          EmbeddingStage.class
      );

  @Override
  public String id() {
    return "embed";
  }

  @Override
  public String fingerprint(Job job) {
    Objects.requireNonNull(
        job,
        "job must not be null"
    );

    List<Chunk> chunks =
        getRequiredChunks(
            job.documentVersionId()
        );

    Map<UUID, Section> sections =
        getSectionsById(
            job.documentVersionId()
        );

    Map<UUID, String> texts =
        composeTexts(
            chunks,
            sections
        );

    Map<UUID, String> fingerprints =
        calculateChunkFingerprints(
            chunks,
            texts
        );

    StringBuilder source =
        new StringBuilder();

    for (Chunk chunk : chunks) {
      Section section =
          sections.get(
              chunk.sectionId()
          );

      if (section == null) {
        throw new IllegalStateException(
            "Section "
                + chunk.sectionId()
                + " not found for chunk "
                + chunk.id()
        );
      }

      source
          .append("section=")
          .append(section.stableKey())
          .append('\n')
          .append("chunkIndex=")
          .append(chunk.chunkIndex())
          .append('\n')
          .append("fingerprint=")
          .append(
              fingerprints.get(
                  chunk.id()
              )
          )
          .append('\n');
    }

    return contentHashService.calculate(
        source.toString()
            .getBytes(StandardCharsets.UTF_8)
    );
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

    List<Chunk> chunks =
        getRequiredChunks(job.documentVersionId());

    Map<UUID, Section> sections =
        getSectionsById(job.documentVersionId());

    Map<UUID, String> texts =
        composeTexts(chunks, sections);

    Map<UUID, String> expected =
        calculateChunkFingerprints(
            chunks,
            texts
        );

    Map<UUID, String> existing =
        embeddingRepository
            .findFingerprintsByDocumentVersionId(
                job.documentVersionId()
            );

    if (existing.size() != expected.size()) {
      return false;
    }

    for (Map.Entry<UUID, String> entry
        : expected.entrySet()) {

      if (!Objects.equals(
          entry.getValue(),
          existing.get(entry.getKey())
      )) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void execute(Job job) {
    Objects.requireNonNull(job, "job must not be null");

    List<Chunk> chunks =
        getRequiredChunks(job.documentVersionId());

    Map<UUID, Section> sections =
        getSectionsById(job.documentVersionId());

    Map<UUID, String> texts =
        composeTexts(chunks, sections);

    Map<UUID, String> expectedFingerprints =
        calculateChunkFingerprints(
            chunks,
            texts
        );

    Map<UUID, String> existingFingerprints =
        embeddingRepository
            .findFingerprintsByDocumentVersionId(
                job.documentVersionId()
            );

    List<Chunk> chunksToEmbed =
        new ArrayList<>();

    List<String> textsToEmbed =
        new ArrayList<>();

    for (Chunk chunk : chunks) {
      String expectedFingerprint =
          expectedFingerprints.get(
              chunk.id()
          );

      String existingFingerprint =
          existingFingerprints.get(
              chunk.id()
          );

      if (Objects.equals(
          expectedFingerprint,
          existingFingerprint
      )) {
        continue;
      }

      boolean reused =
          embeddingRepository
              .reuseFromActiveVersion(
                  chunk.id(),
                  expectedFingerprint
              );

      if (reused) {
        continue;
      }

      chunksToEmbed.add(chunk);

      textsToEmbed.add(
          texts.get(chunk.id())
      );
    }

    if (chunksToEmbed.isEmpty()) {
      return;
    }
    log.info(
        "Embedding chunks: documentVersionId={}, totalChunks={}, providerRequests={}",
        job.documentVersionId(),
        chunks.size(),
        chunksToEmbed.size()
    );
    List<float[]> vectors =
        embeddingProvider.embed(
            textsToEmbed
        );

    if (vectors.size() != chunksToEmbed.size()) {
      throw new IllegalStateException(
          "Embedding provider returned "
              + vectors.size()
              + " vectors for "
              + chunksToEmbed.size()
              + " chunks"
      );
    }

    for (int index = 0;
        index < chunksToEmbed.size();
        index++) {

      Chunk chunk =
          chunksToEmbed.get(index);

      float[] vector =
          vectors.get(index);

      ChunkEmbedding embedding =
          new ChunkEmbedding(
              UUID.randomUUID(),
              chunk.id(),
              embeddingProvider.id(),
              embeddingProvider.model(),
              null,
              embeddingProvider.dimensions(),
              expectedFingerprints.get(
                  chunk.id()
              ),
              vector
          );

      embeddingRepository.upsert(
          embedding
      );
    }
  }

  private List<Chunk> getRequiredChunks(
      UUID documentVersionId
  ) {
    List<Chunk> chunks =
        chunkRepository
            .findByDocumentVersionId(
                documentVersionId
            );

    if (chunks.isEmpty()) {
      throw new DocumentChunksNotFoundException(
          documentVersionId
      );
    }

    return chunks;
  }

  private Map<UUID, Section> getSectionsById(
      UUID documentVersionId
  ) {
    List<Section> sections =
        sectionRepository
            .findByDocumentVersionId(
                documentVersionId
            );

    Map<UUID, Section> result =
        new HashMap<>();

    for (Section section : sections) {
      Section previous =
          result.put(
              section.id(),
              section
          );

      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate section id: "
                + section.id()
        );
      }
    }

    return Map.copyOf(result);
  }

  private Map<UUID, String> composeTexts(
      List<Chunk> chunks,
      Map<UUID, Section> sections
  ) {
    Map<UUID, String> result =
        new HashMap<>();

    for (Chunk chunk : chunks) {
      Section section =
          sections.get(
              chunk.sectionId()
          );

      if (section == null) {
        throw new IllegalStateException(
            "Section "
                + chunk.sectionId()
                + " not found for chunk "
                + chunk.id()
        );
      }

      List<Section> ancestors =
          resolveAncestors(
              section,
              sections
          );

      String text =
          textComposer.compose(
              chunk,
              section,
              ancestors
          );

      result.put(
          chunk.id(),
          text
      );
    }

    return Map.copyOf(result);
  }

  private Map<UUID, String> calculateChunkFingerprints(
      List<Chunk> chunks,
      Map<UUID, String> texts
  ) {
    Map<UUID, String> result =
        new HashMap<>();

    for (Chunk chunk : chunks) {
      String text =
          texts.get(
              chunk.id()
          );

      if (text == null) {
        throw new IllegalStateException(
            "Embedding text not found for chunk: "
                + chunk.id()
        );
      }

      String source =
          "composer=" + textComposer.version()
              + "\nprovider=" + embeddingProvider.id()
              + "\nmodel=" + embeddingProvider.model()
              + "\ndimensions=" + embeddingProvider.dimensions()
              + "\ntext=" + text;

      String fingerprint =
          contentHashService.calculate(
              source.getBytes(
                  StandardCharsets.UTF_8
              )
          );

      result.put(
          chunk.id(),
          fingerprint
      );
    }

    return Map.copyOf(result);
  }

  private List<Section> resolveAncestors(
      Section section,
      Map<UUID, Section> sections
  ) {
    List<Section> ancestors =
        new ArrayList<>();

    Set<UUID> visited =
        new HashSet<>();

    UUID parentId =
        section.parentId();

    while (parentId != null) {
      if (!visited.add(parentId)) {
        throw new IllegalStateException(
            "Cycle detected in section hierarchy at section: "
                + parentId
        );
      }

      Section parent =
          sections.get(parentId);

      if (parent == null) {
        throw new IllegalStateException(
            "Parent section not found: "
                + parentId
        );
      }

      ancestors.add(parent);

      parentId =
          parent.parentId();
    }

    Collections.reverse(ancestors);

    return List.copyOf(ancestors);
  }
}