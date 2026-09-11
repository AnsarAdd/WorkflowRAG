package com.ansaradd.workflowragsrc.chunk.service.impl;

import com.ansaradd.workflowragsrc.chunk.config.ChunkingProperties;
import com.ansaradd.workflowragsrc.chunk.service.TextChunker;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ParagraphTextChunker implements TextChunker {

  private static final String PARAGRAPH_SEPARATOR = "\n\n";

  private final int maxCharacters;

  public ParagraphTextChunker(
      ChunkingProperties properties
  ) {
    this.maxCharacters = properties.maxCharacters();
  }

  @Override
  public List<String> chunk(String content) {
    Objects.requireNonNull(
        content,
        "content must not be null"
    );

    String normalized = normalize(content);

    if (normalized.isBlank()) {
      return List.of();
    }

    List<String> chunks = new ArrayList<>();

    StringBuilder currentChunk =
        new StringBuilder();

    String[] paragraphs =
        normalized.split("\\n[ \\t]*\\n+");

    for (String paragraph : paragraphs) {
      String normalizedParagraph =
          paragraph.strip();

      if (normalizedParagraph.isEmpty()) {
        continue;
      }

      if (normalizedParagraph.length() > maxCharacters) {
        flush(
            currentChunk,
            chunks
        );

        chunks.addAll(
            splitOversizedParagraph(
                normalizedParagraph
            )
        );

        continue;
      }

      if (currentChunk.isEmpty()) {
        currentChunk.append(
            normalizedParagraph
        );
        continue;
      }

      int combinedLength =
          currentChunk.length()
              + PARAGRAPH_SEPARATOR.length()
              + normalizedParagraph.length();

      if (combinedLength <= maxCharacters) {
        currentChunk
            .append(PARAGRAPH_SEPARATOR)
            .append(normalizedParagraph);
      } else {
        flush(
            currentChunk,
            chunks
        );

        currentChunk.append(
            normalizedParagraph
        );
      }
    }

    flush(
        currentChunk,
        chunks
    );

    return List.copyOf(chunks);
  }

  private List<String> splitOversizedParagraph(
      String paragraph
  ) {
    List<String> chunks =
        new ArrayList<>();

    BreakIterator iterator =
        BreakIterator.getSentenceInstance(
            Locale.ROOT
        );

    iterator.setText(paragraph);

    StringBuilder currentChunk =
        new StringBuilder();

    int start = iterator.first();

    for (int end = iterator.next();
        end != BreakIterator.DONE;
        start = end, end = iterator.next()) {

      String sentence =
          paragraph
              .substring(start, end)
              .strip();

      if (sentence.isEmpty()) {
        continue;
      }

      if (sentence.length() > maxCharacters) {
        flush(
            currentChunk,
            chunks
        );

        chunks.addAll(
            hardSplit(sentence)
        );

        continue;
      }

      if (currentChunk.isEmpty()) {
        currentChunk.append(sentence);
        continue;
      }

      int combinedLength =
          currentChunk.length()
              + 1
              + sentence.length();

      if (combinedLength <= maxCharacters) {
        currentChunk
            .append(' ')
            .append(sentence);
      } else {
        flush(
            currentChunk,
            chunks
        );

        currentChunk.append(sentence);
      }
    }

    flush(
        currentChunk,
        chunks
    );

    /*
     * BreakIterator theoretically may not produce a usable
     * sentence for some unusual input.
     */
    if (chunks.isEmpty()) {
      return hardSplit(paragraph);
    }

    return chunks;
  }

  private List<String> hardSplit(
      String value
  ) {
    List<String> chunks =
        new ArrayList<>();

    int offset = 0;

    while (offset < value.length()) {
      int end =
          Math.min(
              offset + maxCharacters,
              value.length()
          );

      if (end < value.length()) {
        int whitespace =
            findLastWhitespace(
                value,
                offset,
                end
            );

        if (whitespace > offset) {
          end = whitespace;
        }
      }

      String chunk =
          value
              .substring(offset, end)
              .strip();

      if (!chunk.isEmpty()) {
        chunks.add(chunk);
      }

      offset = end;

      while (offset < value.length()
          && Character.isWhitespace(
          value.charAt(offset)
      )) {
        offset++;
      }
    }

    return chunks;
  }

  private int findLastWhitespace(
      String value,
      int start,
      int end
  ) {
    for (int index = end - 1;
        index > start;
        index--) {

      if (Character.isWhitespace(
          value.charAt(index)
      )) {
        return index;
      }
    }

    return -1;
  }

  private void flush(
      StringBuilder currentChunk,
      List<String> chunks
  ) {
    if (currentChunk.isEmpty()) {
      return;
    }

    chunks.add(
        currentChunk.toString()
    );

    currentChunk.setLength(0);
  }

  private String normalize(String content) {
    return content
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .strip();
  }
}