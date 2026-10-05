package com.ansaradd.workflowragsrc.context.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.service.impl.DefaultRrfFusionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultRrfFusionServiceTest {

  private final DefaultRrfFusionService service =
      new DefaultRrfFusionService();

  @Test
  void shouldPromoteChunkPresentInBothRankings() {
    RetrievalHit semanticOnly =
        hit(
            UUID.randomUUID(),
            0.95
        );

    RetrievalHit shared =
        hit(
            UUID.randomUUID(),
            0.90
        );

    RetrievalHit lexicalOnly =
        hit(
            UUID.randomUUID(),
            0.80
        );

    List<RetrievalHit> result =
        service.fuse(
            List.of(
                semanticOnly,
                shared
            ),
            List.of(
                shared,
                lexicalOnly
            ),
            3
        );

    assertEquals(
        shared.chunkId(),
        result.getFirst().chunkId()
    );
  }

  @Test
  void shouldReturnEachChunkOnlyOnce() {
    RetrievalHit shared =
        hit(
            UUID.randomUUID(),
            0.90
        );

    List<RetrievalHit> result =
        service.fuse(
            List.of(shared),
            List.of(shared),
            10
        );

    assertEquals(
        1,
        result.size()
    );
  }

  @Test
  void shouldRespectLimit() {
    RetrievalHit first =
        hit(UUID.randomUUID(), 0.9);

    RetrievalHit second =
        hit(UUID.randomUUID(), 0.8);

    RetrievalHit third =
        hit(UUID.randomUUID(), 0.7);

    List<RetrievalHit> result =
        service.fuse(
            List.of(
                first,
                second
            ),
            List.of(
                third
            ),
            2
        );

    assertEquals(
        2,
        result.size()
    );
  }

  @Test
  void shouldNotCountDuplicateWithinSameRankingTwice() {
    RetrievalHit chunk =
        hit(
            UUID.randomUUID(),
            0.90
        );

    List<RetrievalHit> result =
        service.fuse(
            List.of(
                chunk,
                chunk
            ),
            List.of(),
            10
        );

    assertEquals(
        1,
        result.size()
    );

    assertEquals(
        1.0 / 61.0,
        result.getFirst().score(),
        0.0000001
    );
  }

  private RetrievalHit hit(
      UUID chunkId,
      double score
  ) {
    return new RetrievalHit(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        chunkId,
        "source",
        "document",
        "section",
        "content",
        score
    );
  }
}