package com.ansaradd.workflowragsrc.retrieval.service.impl;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.service.RrfFusionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultRrfFusionService
    implements RrfFusionService {

  private static final int RRF_K = 60;

  @Override
  public List<RetrievalHit> fuse(
      List<RetrievalHit> semanticHits,
      List<RetrievalHit> lexicalHits,
      int limit
  ) {
    Objects.requireNonNull(
        semanticHits,
        "semanticHits must not be null"
    );
    Objects.requireNonNull(
        lexicalHits,
        "lexicalHits must not be null"
    );

    if (limit <= 0) {
      throw new IllegalArgumentException(
          "limit must be positive"
      );
    }

    Map<UUID, Candidate> candidates =
        new HashMap<>();

    accumulate(
        candidates,
        semanticHits
    );

    accumulate(
        candidates,
        lexicalHits
    );

    return candidates.values()
        .stream()
        .sorted(
            (left, right) -> {
              int scoreComparison =
                  Double.compare(
                      right.rrfScore(),
                      left.rrfScore()
                  );

              if (scoreComparison != 0) {
                return scoreComparison;
              }

              return left.hit()
                  .chunkId()
                  .compareTo(
                      right.hit().chunkId()
                  );
            }
        )
        .limit(limit)
        .map(this::toRetrievalHit)
        .toList();
  }

  private void accumulate(
      Map<UUID, Candidate> candidates,
      List<RetrievalHit> hits
  ) {
    Map<UUID, Boolean> seen =
        new HashMap<>();

    for (int index = 0;
        index < hits.size();
        index++) {

      RetrievalHit hit =
          Objects.requireNonNull(
              hits.get(index),
              "retrieval hit must not be null"
          );

      /*
       * Defensive protection against a repository accidentally
       * returning the same chunk more than once in one ranking.
       *
       * A single channel must contribute only once per chunk.
       */
      if (seen.putIfAbsent(
          hit.chunkId(),
          Boolean.TRUE
      ) != null) {
        continue;
      }

      int rank =
          index + 1;

      double contribution =
          1.0 / (RRF_K + rank);

      candidates.compute(
          hit.chunkId(),
          (chunkId, current) -> {
            if (current == null) {
              return new Candidate(
                  hit,
                  contribution
              );
            }

            validateSameChunk(
                current.hit(),
                hit
            );

            return new Candidate(
                current.hit(),
                current.rrfScore()
                    + contribution
            );
          }
      );
    }
  }

  private void validateSameChunk(
      RetrievalHit first,
      RetrievalHit second
  ) {
    if (!first.chunkId().equals(
        second.chunkId()
    )) {
      throw new IllegalStateException(
          "Cannot fuse different chunks"
      );
    }

    if (!first.documentId().equals(
        second.documentId()
    )
        || !first.documentVersionId().equals(
        second.documentVersionId()
    )
        || !first.sectionId().equals(
        second.sectionId()
    )) {

      throw new IllegalStateException(
          "Retrieval channels returned inconsistent "
              + "metadata for chunk "
              + first.chunkId()
      );
    }
  }

  private RetrievalHit toRetrievalHit(
      Candidate candidate
  ) {
    RetrievalHit hit =
        candidate.hit();

    return new RetrievalHit(
        hit.documentId(),
        hit.documentVersionId(),
        hit.sectionId(),
        hit.chunkId(),
        hit.sourceId(),
        hit.externalDocumentId(),
        hit.sectionTitle(),
        hit.content(),
        candidate.rrfScore()
    );
  }

  private record Candidate(
      RetrievalHit hit,
      double rrfScore
  ) {
  }
}