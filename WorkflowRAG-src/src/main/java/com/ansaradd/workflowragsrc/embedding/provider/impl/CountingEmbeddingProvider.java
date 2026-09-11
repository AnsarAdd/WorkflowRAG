package com.ansaradd.workflowragsrc.embedding.provider.impl;

import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
import java.util.ArrayList;
import java.util.List;

public class CountingEmbeddingProvider
    implements EmbeddingProvider {

  private static final int DIMENSIONS =
      1024;

  private static final String REVISION =
      "test-revision-v1";

  private final List<String> embeddedTexts =
      new ArrayList<>();

  @Override
  public String id() {
    return "test";
  }

  @Override
  public String model() {
    return "test-v1";
  }

  @Override
  public String revision() {
    return REVISION;
  }

  @Override
  public int dimensions() {
    return DIMENSIONS;
  }

  @Override
  public List<float[]> embed(
      List<String> texts
  ) {
    embeddedTexts.addAll(
        texts
    );

    return texts
        .stream()
        .map(
            this::createVector
        )
        .toList();
  }

  public List<String> embeddedTexts() {
    return List.copyOf(
        embeddedTexts
    );
  }

  public void reset() {
    embeddedTexts.clear();
  }

  private float[] createVector(
      String text
  ) {
    float[] vector =
        new float[DIMENSIONS];

    int index =
        Math.floorMod(
            text.hashCode(),
            DIMENSIONS
        );

    vector[index] =
        1.0f;

    return vector;
  }
}