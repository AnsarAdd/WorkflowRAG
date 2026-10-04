package com.ansaradd.workflowragsrc.document.repository;

import com.ansaradd.workflowragsrc.document.model.Section;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository {

  void replaceForVersion(
      UUID documentVersionId,
      String processingFingerprint,
      List<Section> sections
  );

  boolean hasReusableParse(
      UUID documentVersionId,
      String processingFingerprint
  );

  List<Section> findByDocumentVersionId(
      UUID documentVersionId
  );

  Optional<Section> findById(UUID id);
}