package com.ansaradd.workflowragsrc.workflow.model;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record PipelineTemplate(
    String id,
    List<String> stages
) {

  public PipelineTemplate {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(stages, "stages must not be null");

    id = id.strip();

    if (id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }

    if (stages.isEmpty()) {
      throw new IllegalArgumentException("stages must not be empty");
    }

    List<String> normalizedStages = new ArrayList<>(stages.size());

    for (String stage : stages) {
      Objects.requireNonNull(
          stage,
          "pipeline stage must not be null"
      );

      String normalizedStage = stage.strip();

      if (normalizedStage.isBlank()) {
        throw new IllegalArgumentException(
            "pipeline stage must not be blank"
        );
      }

      normalizedStages.add(normalizedStage);
    }

    if (new HashSet<>(normalizedStages).size()
        != normalizedStages.size()) {
      throw new IllegalArgumentException(
          "pipeline stages must be unique"
      );
    }

    stages = List.copyOf(normalizedStages);
  }
}