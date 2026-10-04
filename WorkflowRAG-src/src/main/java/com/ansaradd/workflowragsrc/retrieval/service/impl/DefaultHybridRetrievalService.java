package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.service.HybridRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.LexicalRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.RrfFusionService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultHybridRetrievalService
    implements HybridRetrievalService {

  private final RetrievalService semanticRetrievalService;
  private final LexicalRetrievalService lexicalRetrievalService;
  private final RrfFusionService rrfFusionService;

  public DefaultHybridRetrievalService(
      RetrievalService semanticRetrievalService,
      LexicalRetrievalService lexicalRetrievalService,
      RrfFusionService rrfFusionService
  ) {
    this.semanticRetrievalService =
        semanticRetrievalService;
    this.lexicalRetrievalService =
        lexicalRetrievalService;
    this.rrfFusionService =
        rrfFusionService;
  }

  @Override
  public List<RetrievalHit> search(
      String query,
      List<String> sourceIds,
      int limit
  ) {
    List<RetrievalHit> semanticHits =
        semanticRetrievalService.search(
            query,
            sourceIds,
            limit
        );

    List<RetrievalHit> lexicalHits =
        lexicalRetrievalService.search(
            query,
            sourceIds,
            limit
        );

    return rrfFusionService.fuse(
        semanticHits,
        lexicalHits,
        limit
    );
  }
}