package com.ansaradd.workflowragsrc.retrieval.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalExecutionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetrievalEvaluatorTest {

  @Mock
  private RetrievalExecutionService retrievalExecutionService;

  @Test
  void shouldCalculateRecallAndMrr() {
    RetrievalEvaluator evaluator =
        new RetrievalEvaluator(
            retrievalExecutionService
        );

    RetrievalEvaluationCase firstCase =
        new RetrievalEvaluationCase(
            "payment service",
            "how are payments processed",
            null,
            "backend-git",
            "PaymentService.java"
        );

    RetrievalEvaluationCase secondCase =
        new RetrievalEvaluationCase(
            "customer service",
            "customer lookup",
            null,
            "backend-git",
            "CustomerService.java"
        );

    when(
        retrievalExecutionService.search(
            firstCase.query(),
            null,
            3,
            RetrievalProfile.SIMPLE
        )
    ).thenReturn(
        List.of(
            hit(
                "backend-git",
                "Other.java"
            ),
            hit(
                "backend-git",
                "PaymentService.java"
            )
        )
    );

    when(
        retrievalExecutionService.search(
            secondCase.query(),
            null,
            3,
            RetrievalProfile.SIMPLE
        )
    ).thenReturn(
        List.of(
            hit(
                "backend-git",
                "Unknown.java"
            )
        )
    );

    RetrievalEvaluationResult result =
        evaluator.evaluate(
            List.of(
                firstCase,
                secondCase
            ),
            RetrievalProfile.SIMPLE,
            3
        );

    assertEquals(
        2,
        result.totalCases()
    );

    assertEquals(
        1,
        result.matchedCases()
    );

    assertEquals(
        0.5,
        result.recallAtK(),
        0.000001
    );

    assertEquals(
        0.25,
        result.mrrAtK(),
        0.000001
    );

    assertEquals(
        2,
        result.cases()
            .getFirst()
            .rank()
    );

    assertEquals(
        null,
        result.cases()
            .get(1)
            .rank()
    );
  }

  private RetrievalHit hit(
      String sourceId,
      String externalDocumentId
  ) {
    return new RetrievalHit(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        sourceId,
        externalDocumentId,
        "section",
        "content",
        0.9
    );
  }
}