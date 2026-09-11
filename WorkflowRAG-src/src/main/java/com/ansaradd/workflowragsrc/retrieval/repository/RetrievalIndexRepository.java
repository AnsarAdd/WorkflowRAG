package com.ansaradd.workflowragsrc.retrieval.repository;

import java.util.UUID;

public interface RetrievalIndexRepository {

  void replaceForVersion(UUID documentVersionId);
}