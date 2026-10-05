package com.ansaradd.workflowragsrc.preflight.service.impl;

import com.ansaradd.workflowragapi.model.response.PreviewChangeSummary;
import com.ansaradd.workflowragapi.model.response.PreviewSimilarityMatch;
import com.ansaradd.workflowragapi.model.response.PreviewSimilarityReport;
import com.ansaradd.workflowragsrc.chunk.service.TextChunker;
import com.ansaradd.workflowragsrc.document.repository.SectionRepository;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.preflight.config.PreflightProperties;
import com.ansaradd.workflowragsrc.preflight.service.PreviewAnalysisService;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.service.LexicalRetrievalService;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalService;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import com.ansaradd.workflowragsrc.source.service.SourceRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DefaultPreviewAnalysisService implements PreviewAnalysisService {
  private final TextChunker textChunker;
  private final SectionRepository sectionRepository;
  private final RetrievalService semanticSearch;
  private final LexicalRetrievalService lexicalSearch;
  private final SourceRegistry sourceRegistry;
  private final PreflightProperties properties;
  private final ExecutorService executor;

  public DefaultPreviewAnalysisService(
      TextChunker textChunker,
      SectionRepository sectionRepository,
      RetrievalService semanticSearch,
      LexicalRetrievalService lexicalSearch,
      SourceRegistry sourceRegistry,
      PreflightProperties properties,
      @Qualifier("preflightSearchExecutor") ExecutorService executor
  ) {
    this.textChunker = textChunker;
    this.sectionRepository = sectionRepository;
    this.semanticSearch = semanticSearch;
    this.lexicalSearch = lexicalSearch;
    this.sourceRegistry = sourceRegistry;
    this.properties = properties;
    this.executor = executor;
  }

  @Override
  public PreviewChangeSummary changes(ParsedDocument document, UUID baselineVersionId) {
    List<String> incoming = parts(document);
    List<String> previous = baselineVersionId == null ? List.of()
        : sectionRepository.findByDocumentVersionId(baselineVersionId).stream()
            .flatMap(section -> textChunker.chunk(section.content()).stream()).toList();
    Map<String, Integer> available = new HashMap<>();
    previous.forEach(part -> available.merge(part, 1, Integer::sum));
    int unchanged = 0;
    for (String part : incoming) {
      int count = available.getOrDefault(part, 0);
      if (count > 0) {
        unchanged++;
        available.put(part, count - 1);
      }
    }
    return new PreviewChangeSummary(previous.size(), incoming.size(), unchanged,
        incoming.size() - unchanged, previous.size() - unchanged);
  }

  @Override
  public PreviewSimilarityReport similarity(ParsedDocument document, DocumentKey key) {
    List<String> parts = parts(document);
    List<String> queries = new ArrayList<>();
    int sampleCount = Math.min(parts.size(), properties.similaritySamples());
    for (int index = 0; index < sampleCount; index++) {
      int position = sampleCount == 1 ? 0 : index * (parts.size() - 1) / (sampleCount - 1);
      queries.add(shorten(parts.get(position), 1000));
    }
    List<String> sources = sourceRegistry.findAll().stream().map(source -> source.id()).toList();
    List<PreviewSimilarityMatch> matches = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(properties.searchTimeoutSeconds());
    Future<List<PreviewSimilarityMatch>> semantic = submit(
        queries, query -> semanticSearch.search(query, sources, 10), "SEMANTIC", key);
    Future<List<PreviewSimilarityMatch>> lexical = submit(
        queries, query -> lexicalSearch.search(query, sources, 10), "LEXICAL", key);
    collect(semantic, "SEMANTIC", deadline, matches, warnings);
    collect(lexical, "LEXICAL", deadline, matches, warnings);
    String status = warnings.isEmpty() ? "COMPLETE" : warnings.size() == 2 ? "UNAVAILABLE" : "PARTIAL";
    return new PreviewSimilarityReport(status, sampleCount, List.copyOf(matches), List.copyOf(warnings));
  }

  private Future<List<PreviewSimilarityMatch>> submit(
      List<String> queries,
      Function<String, List<RetrievalHit>> search,
      String method,
      DocumentKey key
  ) {
    try {
      return executor.submit(() -> {
        Map<UUID, PreviewSimilarityMatch> result = new LinkedHashMap<>();
        for (String query : queries) {
          if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Preview search cancelled");
          }
          for (RetrievalHit hit : search.apply(query)) {
            if (hit.sourceId().equals(key.sourceId())
                && hit.externalDocumentId().equals(key.externalDocumentId())) {
              continue;
            }
            result.putIfAbsent(hit.chunkId(), new PreviewSimilarityMatch(hit.documentVersionId(),
                hit.sourceId(), hit.externalDocumentId(), hit.sectionTitle(), shorten(hit.content(), 400),
                method, hit.score()));
          }
        }
        return result.values().stream().limit(10).toList();
      });
    } catch (java.util.concurrent.RejectedExecutionException exception) {
      return null;
    }
  }

  private void collect(
      Future<List<PreviewSimilarityMatch>> future,
      String method,
      long deadline,
      List<PreviewSimilarityMatch> matches,
      List<String> warnings
  ) {
    if (future == null) {
      warnings.add(method + " search capacity exhausted; similarity is incomplete");
      return;
    }
    try {
      matches.addAll(future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      warnings.add(method + " search interrupted; similarity is incomplete");
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
      future.cancel(true);
      warnings.add(method + " search unavailable or timed out; similarity is incomplete");
    }
  }

  private List<String> parts(ParsedDocument document) {
    return document.sections().stream()
        .flatMap(section -> textChunker.chunk(section.content()).stream()).toList();
  }

  private String shorten(String text, int limit) {
    return text.length() <= limit ? text : text.substring(0, limit);
  }
}
