package com.ansaradd.workflowragsrc.context.service.impl;

import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.chunk.repository.ChunkRepository;
import com.ansaradd.workflowragsrc.context.config.ContextProperties;
import com.ansaradd.workflowragsrc.context.model.ContextExpansionMode;
import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
import com.ansaradd.workflowragsrc.context.service.ContextResolver;
import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultContextResolver
    implements ContextResolver {

  private final SectionRepository sectionRepository;
  private final ChunkRepository chunkRepository;
  private final ContextProperties properties;

  public DefaultContextResolver(
      SectionRepository sectionRepository,
      ChunkRepository chunkRepository,
      ContextProperties properties
  ) {
    this.sectionRepository = sectionRepository;
    this.chunkRepository = chunkRepository;
    this.properties = properties;
  }

  @Override
  public List<ResolvedContext> resolve(
      List<RetrievalHit> hits,
      ContextExpansionStrategy strategy
  ) {
    Objects.requireNonNull(
        hits,
        "hits must not be null"
    );
    Objects.requireNonNull(
        strategy,
        "strategy must not be null"
    );

    Map<ContextIdentity, ResolvedContext> resolved =
        new LinkedHashMap<>();

    for (RetrievalHit hit : hits) {
      Objects.requireNonNull(
          hit,
          "retrieval hit must not be null"
      );

      ResolvedCandidate candidate =
          switch (strategy) {
            case CHUNK_ONLY ->
                resolveChunkOnly(hit);

            case SECTION_IF_SMALL ->
                resolveSectionIfSmall(hit);

            case NEIGHBOR_CHUNKS ->
                resolveNeighborChunks(hit);
          };

      resolved.merge(
          candidate.identity(),
          candidate.context(),
          this::keepHigherRetrievalScore
      );
    }

    return List.copyOf(
        resolved.values()
    );
  }

  private ResolvedCandidate resolveChunkOnly(
      RetrievalHit hit
  ) {
    ResolvedContext context =
        new ResolvedContext(
            hit.documentId(),
            hit.documentVersionId(),
            hit.sectionId(),
            hit.chunkId(),
            hit.sourceId(),
            hit.externalDocumentId(),
            hit.sectionTitle(),
            hit.content(),
            hit.score(),
            ResolvedContext.Scope.CHUNK,
            List.of(hit.chunkId())
        );

    return new ResolvedCandidate(
        new ContextIdentity(
            ResolvedContext.Scope.CHUNK,
            hit.chunkId()
        ),
        context
    );
  }

  private ResolvedCandidate resolveSectionIfSmall(
      RetrievalHit hit
  ) {
    Section section =
        sectionRepository.findById(
                hit.sectionId()
            )
            .orElseThrow(
                () -> new IllegalStateException(
                    "Section not found for retrieval hit: "
                        + hit.sectionId()
                )
            );

    validateSection(
        hit,
        section
    );

    String sectionContent =
        Objects.requireNonNull(
            section.content(),
            "section content must not be null"
        );

    if (!isSmallSection(sectionContent)) {
      return resolveChunkOnly(hit);
    }

    ResolvedContext context =
        new ResolvedContext(
            hit.documentId(),
            hit.documentVersionId(),
            section.id(),
            hit.chunkId(),
            hit.sourceId(),
            hit.externalDocumentId(),
            section.title(),
            sectionContent,
            hit.score(),
            ResolvedContext.Scope.SECTION,
            List.of()
        );

    return new ResolvedCandidate(
        new ContextIdentity(
            ResolvedContext.Scope.SECTION,
            section.id()
        ),
        context
    );
  }

  private ResolvedCandidate resolveNeighborChunks(
      RetrievalHit hit
  ) {
    Chunk originChunk =
        chunkRepository.findById(
                hit.chunkId()
            )
            .orElseThrow(
                () -> new IllegalStateException(
                    "Chunk not found for retrieval hit: "
                        + hit.chunkId()
                )
            );

    validateOriginChunk(
        hit,
        originChunk
    );

    List<Chunk> neighbors =
        new ArrayList<>(
            chunkRepository.findNeighbors(
                originChunk.sectionId(),
                originChunk.chunkIndex(),
                properties.neighborDistance()
            )
        );

    neighbors.sort(
        Comparator.comparingInt(
            Chunk::chunkIndex
        )
    );

    boolean containsOrigin =
        neighbors.stream()
            .anyMatch(
                chunk -> chunk.id().equals(
                    originChunk.id()
                )
            );

    if (!containsOrigin) {
      throw new IllegalStateException(
          "Neighbor lookup did not return origin chunk: "
              + originChunk.id()
      );
    }

    String content =
        neighbors.stream()
            .map(Chunk::content)
            .reduce(
                (left, right) ->
                    left + "\n\n" + right
            )
            .orElseThrow(
                () -> new IllegalStateException(
                    "Neighbor lookup returned no chunks for: "
                        + originChunk.id()
                )
            );

    List<UUID> includedChunkIds =
        neighbors.stream()
            .map(Chunk::id)
            .toList();

    ResolvedContext context =
        new ResolvedContext(
            hit.documentId(),
            hit.documentVersionId(),
            hit.sectionId(),
            hit.chunkId(),
            hit.sourceId(),
            hit.externalDocumentId(),
            hit.sectionTitle(),
            content,
            hit.score(),
            ResolvedContext.Scope.WINDOW,
            includedChunkIds
        );

    return new ResolvedCandidate(
        new ContextIdentity(
            ResolvedContext.Scope.WINDOW,
            hit.chunkId()
        ),
        context
    );
  }

  private boolean isSmallSection(
      String content
  ) {
    int chars =
        content.codePointCount(
            0,
            content.length()
        );

    return chars
        <= properties.smallSectionMaxChars();
  }

  private void validateSection(
      RetrievalHit hit,
      Section section
  ) {
    if (!section.id().equals(
        hit.sectionId()
    )) {
      throw new IllegalStateException(
          "Resolved section does not match retrieval hit section"
      );
    }

    if (!section.documentVersionId().equals(
        hit.documentVersionId()
    )) {
      throw new IllegalStateException(
          "Resolved section belongs to another document version: "
              + section.id()
      );
    }
  }

  private void validateOriginChunk(
      RetrievalHit hit,
      Chunk chunk
  ) {
    if (!chunk.id().equals(
        hit.chunkId()
    )) {
      throw new IllegalStateException(
          "Resolved chunk does not match retrieval hit chunk"
      );
    }

    if (!chunk.sectionId().equals(
        hit.sectionId()
    )) {
      throw new IllegalStateException(
          "Resolved chunk belongs to another section: "
              + chunk.id()
      );
    }
  }

  private ResolvedContext keepHigherRetrievalScore(
      ResolvedContext current,
      ResolvedContext candidate
  ) {
    if (candidate.retrievalScore()
        > current.retrievalScore()) {
      return candidate;
    }

    return current;
  }

  private record ContextIdentity(
      ResolvedContext.Scope scope,
      UUID id
  ) {
  }

  private record ResolvedCandidate(
      ContextIdentity identity,
      ResolvedContext context
  ) {
  }
}