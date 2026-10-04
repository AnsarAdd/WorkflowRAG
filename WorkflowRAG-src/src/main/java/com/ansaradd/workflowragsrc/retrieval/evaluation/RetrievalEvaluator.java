package com.ansaradd.workflowragsrc.retrieval.evaluation;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalHit;
import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import com.ansaradd.workflowragsrc.retrieval.service.RetrievalExecutionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RetrievalEvaluator {

  private final RetrievalExecutionService retrievalExecutionService;

  public RetrievalEvaluator(
      RetrievalExecutionService retrievalExecutionService
  ) {
    this.retrievalExecutionService =
        Objects.requireNonNull(
            retrievalExecutionService,
            "retrievalExecutionService must not be null"
        );
  }

  public RetrievalEvaluationResult evaluate(
      List<RetrievalEvaluationCase> cases,
      RetrievalProfile profile,
      int limit
  ) {
    Objects.requireNonNull(
        cases,
        "cases must not be null"
    );
    Objects.requireNonNull(
        profile,
        "profile must not be null"
    );

    if (cases.isEmpty()) {
      throw new IllegalArgumentException(
          "cases must not be empty"
      );
    }

    if (limit <= 0) {
      throw new IllegalArgumentException(
          "limit must be positive"
      );
    }

    List<RetrievalEvaluationResult.CaseResult>
        caseResults = new ArrayList<>();

    int matchedCases = 0;
    double reciprocalRankSum = 0.0;

    for (RetrievalEvaluationCase evaluationCase : cases) {
      Objects.requireNonNull(
          evaluationCase,
          "evaluation case must not be null"
      );

      List<RetrievalHit> hits =
          retrievalExecutionService.search(
              evaluationCase.query(),
              evaluationCase.searchSourceId(),
              limit,
              profile
          );

      Integer rank =
          findExpectedRank(
              evaluationCase,
              hits
          );

      if (rank != null) {
        matchedCases++;
        reciprocalRankSum +=
            1.0 / rank;
      }

      caseResults.add(
          new RetrievalEvaluationResult.CaseResult(
              evaluationCase.name(),
              evaluationCase.query(),
              rank
          )
      );
    }

    double recallAtK =
        (double) matchedCases
            / cases.size();

    double mrrAtK =
        reciprocalRankSum
            / cases.size();

    return new RetrievalEvaluationResult(
        profile,
        limit,
        cases.size(),
        matchedCases,
        recallAtK,
        mrrAtK,
        caseResults
    );
  }

  private Integer findExpectedRank(
      RetrievalEvaluationCase evaluationCase,
      List<RetrievalHit> hits
  ) {
    for (int index = 0;
        index < hits.size();
        index++) {

      RetrievalHit hit =
          hits.get(index);

      if (matches(
          evaluationCase,
          hit
      )) {
        return index + 1;
      }
    }

    return null;
  }

  private boolean matches(
      RetrievalEvaluationCase evaluationCase,
      RetrievalHit hit
  ) {
    return evaluationCase.expectedSourceId()
        .equals(hit.sourceId())
        && evaluationCase.expectedExternalDocumentId()
        .equals(hit.externalDocumentId());
  }
}