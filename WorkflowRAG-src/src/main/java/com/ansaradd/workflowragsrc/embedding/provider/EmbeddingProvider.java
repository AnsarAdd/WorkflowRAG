package com.ansaradd.workflowragsrc.embedding.provider;

import java.util.List;

public interface EmbeddingProvider {

  String id();

  String model();

  String revision();

  int dimensions();

  List<float[]> embed(List<String> texts);
}