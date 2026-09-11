package com.ansaradd.workflowragsrc.embedding.service;

import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.document.model.Section;
import java.util.List;

public interface EmbeddingTextComposer {

  String version();

  String compose(
      Chunk chunk,
      Section section,
      List<Section> ancestors
  );
}