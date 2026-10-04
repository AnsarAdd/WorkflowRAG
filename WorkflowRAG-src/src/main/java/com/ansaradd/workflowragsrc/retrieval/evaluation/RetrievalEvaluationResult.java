package com.ansaradd.workflowragsrc.retrieval.evaluation;

import com.ansaradd.workflowragsrc.retrieval.model.RetrievalProfile;
import java.util.List;
import java.util.Objects;

public record RetrievalEvaluationResult(
    RetrievalProfile profile,
    int limit,
    int totalCases,
    int matchedCases,
    double recallAtK,
    double mrrAtK,
    List<CaseResult> cases
) {

  public RetrievalEvaluationResult {
    Objects.requireNonNull(
        profile,
        "profile must not be null"
    );
    Objects.requireNonNull(
        cases,
        "cases must not be null"
    );

    cases = List.copyOf(cases);
  }

  public record CaseResult(
      String name,
      String query,
      Integer rank
  ) {

    public boolean matched() {
      return rank != null;
    }
  }
}