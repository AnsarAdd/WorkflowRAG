package com.ansaradd.workflowragsrc.retrieval.controller;

import com.ansaradd.workflowragapi.controller.RetrievalController;
import com.ansaradd.workflowragapi.model.request.RetrievalSearchRequest;
import com.ansaradd.workflowragapi.model.response.RetrievalHitResponse;
import com.ansaradd.workflowragapi.model.response.RetrievalSearchResponse;
import com.ansaradd.workflowragsrc.retrieval.config.RetrievalProperties;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetrievalControllerImpl
    implements RetrievalController {

  private final RetrievalService retrievalService;
  private final RetrievalProperties properties;

  public RetrievalControllerImpl(
      RetrievalService retrievalService,
      RetrievalProperties properties
  ) {
    this.retrievalService = retrievalService;
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
        request.limit() == null
            ? properties.defaultLimit()
            : request.limit();

    List<RetrievalHitResponse> hits =
        retrievalService
            .search(
                request.query(),
                request.sourceId(),
                limit
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
}