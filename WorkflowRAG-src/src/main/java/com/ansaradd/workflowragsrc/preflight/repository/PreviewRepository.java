package com.ansaradd.workflowragsrc.preflight.repository;

import com.ansaradd.workflowragsrc.preflight.model.PreviewSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface PreviewRepository {
  void save(PreviewSnapshot snapshot);
  Optional<PreviewSnapshot> find(UUID id, boolean lock);
  void confirm(UUID id, UUID jobId);
  void lockDocument(UUID documentId);
  void deleteExpired();
}
