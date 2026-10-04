//package com.ansaradd.workflowragsrc.context.service.impl;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
//import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
//import com.ansaradd.workflowragsrc.context.service.ContextResolver;
//import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
//import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
//import java.util.List;
//import java.util.UUID;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultContextRetrievalServiceTest {
//
//  @Mock
//  private RetrievalService retrievalService;
//
//  @Mock
//  private ContextResolver contextResolver;
//
//  @Test
//  void shouldSearchAndResolveContext() {
//    DefaultContextRetrievalService service =
//        new DefaultContextRetrievalService(
//            retrievalService,
//            contextResolver
//        );
//
//    UUID documentId = UUID.randomUUID();
//    UUID versionId = UUID.randomUUID();
//    UUID sectionId = UUID.randomUUID();
//    UUID chunkId = UUID.randomUUID();
//
//    RetrievalHit hit =
//        new RetrievalHit(
//            documentId,
//            versionId,
//            sectionId,
//            chunkId,
//            "source",
//            "document",
//            "section",
//            "chunk",
//            0.91
//        );
//
//    ResolvedContext context =
//        new ResolvedContext(
//            documentId,
//            versionId,
//            sectionId,
//            chunkId,
//            "source",
//            "document",
//            "section",
//            "expanded context",
//            0.91,
//            ResolvedContext.Scope.WINDOW,
//            List.of(chunkId)
//        );
//
//    when(
//        retrievalService.search(
//            "payment",
//            "source",
//            5
//        )
//    ).thenReturn(
//        List.of(hit)
//    );
//
//    when(
//        contextResolver.resolve(
//            List.of(hit),
//            ContextExpansionStrategy.NEIGHBOR_CHUNKS
//        )
//    ).thenReturn(
//        List.of(context)
//    );
//
//    List<ResolvedContext> result =
//        service.retrieve(
//            "payment",
//            "source",
//            5,
//            ContextExpansionStrategy.NEIGHBOR_CHUNKS
//        );
//
//    assertEquals(
//        List.of(context),
//        result
//    );
//
//    verify(
//        retrievalService
//    ).search(
//        "payment",
//        "source",
//        5
//    );
//
//    verify(
//        contextResolver
//    ).resolve(
//        List.of(hit),
//        ContextExpansionStrategy.NEIGHBOR_CHUNKS
//    );
//  }
//}