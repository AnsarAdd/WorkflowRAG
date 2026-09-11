package com.ansaradd.workflowragsrc.document.repository;

import com.ansaradd.workflowragsrc.document.model.StoredDocument;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import java.util.Optional;

public interface DocumentRepository {

  Optional<StoredDocument> findByKey(DocumentKey documentKey);

  StoredDocument getOrCreate(DocumentKey documentKey);
}