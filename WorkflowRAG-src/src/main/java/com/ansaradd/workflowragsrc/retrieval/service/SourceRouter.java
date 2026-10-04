package com.ansaradd.workflowragsrc.retrieval.service;

import java.util.List;

public interface SourceRouter {

  List<String> route(String query);
}