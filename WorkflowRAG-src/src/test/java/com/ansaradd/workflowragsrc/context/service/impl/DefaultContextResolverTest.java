//package com.ansaradd.workflowragsrc.context.service.impl;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoInteractions;
//import static org.mockito.Mockito.when;
//
//import com.ansaradd.workflowragsrc.chunk.model.Chunk;
//import com.ansaradd.workflowragsrc.chunk.repository.ChunkRepository;
//import com.ansaradd.workflowragsrc.context.config.ContextProperties;
//import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
//import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
//import com.ansaradd.workflowragsrc.document.model.Section;
//import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
//import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultContextResolverTest {
//
//  private static final UUID DOCUMENT_ID =
//      UUID.randomUUID();
//
//  private static final UUID VERSION_ID =
//      UUID.randomUUID();
//
//  @Mock
//  private SectionRepository sectionRepository;
//
//  @Mock
//  private ChunkRepository chunkRepository;
//
//  private DefaultContextResolver resolver;
//
//  @BeforeEach
//  void setUp() {
//    resolver =
//        new DefaultContextResolver(
//            sectionRepository,
//            chunkRepository,
//            new ContextProperties(
//                20,
//                1
//            )
//        );
//  }
//
//  @Test
//  void shouldResolveChunkOnlyWithoutRepositoryAccess() {
//    UUID sectionId = UUID.randomUUID();
//    UUID chunkId = UUID.randomUUID();
//
//    RetrievalHit hit =
//        hit(
//            sectionId,
//            chunkId,
//            "chunk content",
//            0.91
//        );
//
//    List<ResolvedContext> result =
//        resolver.resolve(
//            List.of(hit),
//            ContextExpansionStrategy.CHUNK_ONLY
//        );
//
//    assertEquals(1, result.size());
//
//    ResolvedContext context =
//        result.getFirst();
//
//    assertEquals(
//        ResolvedContext.Scope.CHUNK,
//        context.scope()
//    );
//    assertEquals(
//        "chunk content",
//        context.content()
//    );
//    assertEquals(
//        chunkId,
//        context.originChunkId()
//    );
//    assertEquals(
//        List.of(chunkId),
//        context.includedChunkIds()
//    );
//    assertEquals(
//        0.91,
//        context.retrievalScore()
//    );
//
//    verifyNoInteractions(
//        sectionRepository,
//        chunkRepository
//    );
//  }
//
//  @Test
//  void shouldResolveSmallSectionAndDeduplicateHits() {
//    UUID sectionId = UUID.randomUUID();
//    UUID firstChunkId = UUID.randomUUID();
//    UUID secondChunkId = UUID.randomUUID();
//
//    RetrievalHit first =
//        hit(
//            sectionId,
//            firstChunkId,
//            "first",
//            0.80
//        );
//
//    RetrievalHit second =
//        hit(
//            sectionId,
//            secondChunkId,
//            "second",
//            0.95
//        );
//
//    Section section =
//        section(
//            sectionId,
//            "whole section"
//        );
//
//    when(
//        sectionRepository.findById(
//            sectionId
//        )
//    ).thenReturn(
//        Optional.of(section)
//    );
//
//    List<ResolvedContext> result =
//        resolver.resolve(
//            List.of(first, second),
//            ContextExpansionStrategy.SECTION_IF_SMALL
//        );
//
//    assertEquals(1, result.size());
//
//    ResolvedContext context =
//        result.getFirst();
//
//    assertEquals(
//        ResolvedContext.Scope.SECTION,
//        context.scope()
//    );
//    assertEquals(
//        "whole section",
//        context.content()
//    );
//    assertEquals(
//        secondChunkId,
//        context.originChunkId()
//    );
//    assertEquals(
//        0.95,
//        context.retrievalScore()
//    );
//    assertEquals(
//        List.of(),
//        context.includedChunkIds()
//    );
//  }
//
//  @Test
//  void shouldFallbackToChunkWhenSectionIsTooLarge() {
//    UUID sectionId = UUID.randomUUID();
//    UUID chunkId = UUID.randomUUID();
//
//    RetrievalHit hit =
//        hit(
//            sectionId,
//            chunkId,
//            "matching chunk",
//            0.88
//        );
//
//    Section section =
//        section(
//            sectionId,
//            "this section is definitely longer than twenty characters"
//        );
//
//    when(
//        sectionRepository.findById(
//            sectionId
//        )
//    ).thenReturn(
//        Optional.of(section)
//    );
//
//    List<ResolvedContext> result =
//        resolver.resolve(
//            List.of(hit),
//            ContextExpansionStrategy.SECTION_IF_SMALL
//        );
//
//    ResolvedContext context =
//        result.getFirst();
//
//    assertEquals(
//        ResolvedContext.Scope.CHUNK,
//        context.scope()
//    );
//    assertEquals(
//        "matching chunk",
//        context.content()
//    );
//    assertEquals(
//        List.of(chunkId),
//        context.includedChunkIds()
//    );
//  }
//
//  @Test
//  void shouldResolveNeighborWindowInChunkOrder() {
//    UUID sectionId = UUID.randomUUID();
//
//    UUID leftId = UUID.randomUUID();
//    UUID originId = UUID.randomUUID();
//    UUID rightId = UUID.randomUUID();
//
//    RetrievalHit hit =
//        hit(
//            sectionId,
//            originId,
//            "origin",
//            0.92
//        );
//
//    Chunk origin =
//        chunk(
//            originId,
//            sectionId,
//            4,
//            "origin"
//        );
//
//    Chunk left =
//        chunk(
//            leftId,
//            sectionId,
//            3,
//            "left"
//        );
//
//    Chunk right =
//        chunk(
//            rightId,
//            sectionId,
//            5,
//            "right"
//        );
//
//    when(
//        chunkRepository.findById(
//            originId
//        )
//    ).thenReturn(
//        Optional.of(origin)
//    );
//
//    when(
//        chunkRepository.findNeighbors(
//            sectionId,
//            4,
//            1
//        )
//    ).thenReturn(
//        List.of(
//            right,
//            origin,
//            left
//        )
//    );
//
//    List<ResolvedContext> result =
//        resolver.resolve(
//            List.of(hit),
//            ContextExpansionStrategy.NEIGHBOR_CHUNKS
//        );
//
//    ResolvedContext context =
//        result.getFirst();
//
//    assertEquals(
//        ResolvedContext.Scope.WINDOW,
//        context.scope()
//    );
//
//    assertEquals(
//        "left\n\norigin\n\nright",
//        context.content()
//    );
//
//    assertEquals(
//        List.of(
//            leftId,
//            originId,
//            rightId
//        ),
//        context.includedChunkIds()
//    );
//
//    verify(
//        chunkRepository
//    ).findNeighbors(
//        sectionId,
//        4,
//        1
//    );
//  }
//
//  private RetrievalHit hit(
//      UUID sectionId,
//      UUID chunkId,
//      String content,
//      double score
//  ) {
//    return new RetrievalHit(
//        DOCUMENT_ID,
//        VERSION_ID,
//        sectionId,
//        chunkId,
//        "test-source",
//        "test-document",
//        "Test section",
//        content,
//        score
//    );
//  }
//
//  private Section section(
//      UUID sectionId,
//      String content
//  ) {
//    return new Section(
//        sectionId,
//        VERSION_ID,
//        "section-key",
//        null,
//        0,
//        "Test section",
//        1,
//        content,
//        "section-hash"
//    );
//  }
//
//  private Chunk chunk(
//      UUID id,
//      UUID sectionId,
//      int index,
//      String content
//  ) {
//    return new Chunk(
//        id,
//        sectionId,
//        index,
//        content,
//        "hash-" + index,
//        false
//    );
//  }
//}