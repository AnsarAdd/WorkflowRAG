package com.ansaradd.workflowragsrc.retrieval.controller;

import com.ansaradd.workflowragapi.controller.RetrievalController;
import com.ansaradd.workflowragapi.model.enums.ResolvedContextScope;
import com.ansaradd.workflowragapi.model.request.RetrievalContextRequest;
import com.ansaradd.workflowragapi.model.request.RetrievalSearchRequest;
import com.ansaradd.workflowragapi.model.response.ResolvedContextResponse;
import com.ansaradd.workflowragapi.model.response.RetrievalContextResponse;
import com.ansaradd.workflowragapi.model.response.RetrievalHitResponse;
import com.ansaradd.workflowragapi.model.response.RetrievalSearchResponse;
import com.ansaradd.workflowragsrc.context.model.ContextExpansionStrategy;
import com.ansaradd.workflowragsrc.context.model.ResolvedContext;
import com.ansaradd.workflowragsrc.context.service.ContextRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.config.RetrievalProperties;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalExecutionService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetrievalControllerImpl
    implements RetrievalController {

  private final RetrievalExecutionService retrievalExecutionService;
  private final ContextRetrievalService contextRetrievalService;
  private final RetrievalProperties properties;

  public RetrievalControllerImpl(
      RetrievalExecutionService retrievalExecutionService,
      ContextRetrievalService contextRetrievalService,
      RetrievalProperties properties
  ) {
    this.retrievalExecutionService =
        retrievalExecutionService;
    this.contextRetrievalService =
        contextRetrievalService;
    this.properties = properties;
  }

  @Override
  public ResponseEntity<RetrievalSearchResponse> search(
      RetrievalSearchRequest request
  ) {
    Objects.requireNonNull(
        request,
        "request must not be null"
    );

    int limit =
        resolveLimit(
            request.limit()
        );

    RetrievalProfile profile =
        mapProfile(
            request.profile()
        );

    List<RetrievalHitResponse> hits =
        retrievalExecutionService
            .search(
                request.query(),
                request.sourceId(),
                limit,
                profile
            )
            .stream()
            .map(this::mapHit)
            .toList();

    return ResponseEntity.ok(
        new RetrievalSearchResponse(
            hits
        )
    );
  }

  @Override
  public ResponseEntity<RetrievalContextResponse> context(
      RetrievalContextRequest request
  ) {
    Objects.requireNonNull(
        request,
        "request must not be null"
    );

    int limit =
        resolveLimit(
            request.limit()
        );

    RetrievalProfile profile =
        mapProfile(
            request.profile()
        );

    ContextExpansionStrategy strategy =
        mapStrategy(
            request.expansionStrategy()
        );

    List<ResolvedContextResponse> contexts =
        contextRetrievalService
            .retrieve(
                request.query(),
                request.sourceId(),
                limit,
                profile,
                strategy
            )
            .stream()
            .map(this::mapContext)
            .toList();

    return ResponseEntity.ok(
        new RetrievalContextResponse(
            contexts
        )
    );
  }

  private int resolveLimit(
      Integer requestedLimit
  ) {
    return requestedLimit == null
        ? properties.defaultLimit()
        : requestedLimit;
  }

  private RetrievalProfile mapProfile(
      com.ansaradd.workflowragapi.model.enums.RetrievalProfile
          profile
  ) {
    if (profile == null) {
      return RetrievalProfile.SIMPLE;
    }

    return switch (profile) {
      case SIMPLE ->
          RetrievalProfile.SIMPLE;

      case HYBRID ->
          RetrievalProfile.HYBRID;

      case ADVANCED ->
          RetrievalProfile.ADVANCED;
    };
  }

  private ContextExpansionStrategy mapStrategy(
      com.ansaradd.workflowragapi.model.enums.ContextExpansionStrategy
          strategy
  ) {
    if (strategy == null) {
      return ContextExpansionStrategy.CHUNK_ONLY;
    }

    return switch (strategy) {
      case CHUNK_ONLY ->
          ContextExpansionStrategy.CHUNK_ONLY;

      case SECTION_IF_SMALL ->
          ContextExpansionStrategy.SECTION_IF_SMALL;

      case NEIGHBOR_CHUNKS ->
          ContextExpansionStrategy.NEIGHBOR_CHUNKS;
    };
  }

  private RetrievalHitResponse mapHit(
      RetrievalHit hit
  ) {
    return new RetrievalHitResponse(
        hit.documentId(),
        hit.documentVersionId(),
        hit.sectionId(),
        hit.chunkId(),
        hit.sourceId(),
        hit.externalDocumentId(),
        hit.sectionTitle(),
        hit.content(),
        hit.score()
    );
  }

  private ResolvedContextResponse mapContext(
      ResolvedContext context
  ) {
    return new ResolvedContextResponse(
        context.documentId(),
        context.documentVersionId(),
        context.sectionId(),
        context.originChunkId(),
        context.sourceId(),
        context.externalDocumentId(),
        context.sectionTitle(),
        context.content(),
        context.retrievalScore(),
        mapScope(
            context.scope()
        ),
        context.includedChunkIds()
    );
  }

  private ResolvedContextScope mapScope(
      ResolvedContext.Scope scope
  ) {
    return switch (scope) {
      case CHUNK ->
          ResolvedContextScope.CHUNK;

      case SECTION ->
          ResolvedContextScope.SECTION;

      case WINDOW ->
          ResolvedContextScope.WINDOW;
    };
  }
}