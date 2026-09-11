package com.ansaradd.workflowragsrc.embedding.service.impl;

import com.ansaradd.workflowragsrc.chunk.model.Chunk;
import com.ansaradd.workflowragsrc.document.model.Section;
import com.ansaradd.workflowragsrc.embedding.service.EmbeddingTextComposer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultEmbeddingTextComposer
    implements EmbeddingTextComposer {

  private static final String VERSION =
      "embedding-input-v1";

  @Override
  public String version() {
    return VERSION;
  }

  @Override
  public String compose(
      Chunk chunk,
      Section section,
      List<Section> ancestors
  ) {
    Objects.requireNonNull(
        chunk,
        "chunk must not be null"
    );
    Objects.requireNonNull(
        section,
        "section must not be null"
    );
    Objects.requireNonNull(
        ancestors,
        "ancestors must not be null"
    );

    if (!chunk.sectionId().equals(section.id())) {
      throw new IllegalArgumentException(
          "Chunk "
              + chunk.id()
              + " does not belong to section "
              + section.id()
      );
    }

    List<String> titles =
        new ArrayList<>();

    for (Section ancestor : ancestors) {
      Objects.requireNonNull(
          ancestor,
          "ancestor section must not be null"
      );

      addTitle(
          titles,
          ancestor.title()
      );
    }

    addTitle(
        titles,
        section.title()
    );

    String content =
        chunk.content().strip();

    if (content.isEmpty()) {
      throw new IllegalArgumentException(
          "chunk content must not be blank"
      );
    }

    if (titles.isEmpty()) {
      return content;
    }

    return String.join(
        " > ",
        titles
    ) + "\n\n" + content;
  }

  private void addTitle(
      List<String> titles,
      String title
  ) {
    if (title == null) {
      return;
    }

    String normalized =
        title.strip();

    if (!normalized.isEmpty()) {
      titles.add(normalized);
    }
  }
}