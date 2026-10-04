package com.ansaradd.workflowragsrc.reranking.provider;

import java.util.List;

public interface RerankingProvider {

  String id();

  String model();

  String revision();

  List<Double> score(
      String query,
      List<String> documents
  );
}