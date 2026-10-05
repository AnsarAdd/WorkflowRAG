package com.ansaradd.workflowragsrc.chunk.service.impl;

import com.ansaradd.workflowragsrc.chunk.config.ChunkingProperties;
import com.ansaradd.workflowragsrc.chunk.service.TextChunker;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ParagraphTextChunker implements TextChunker {

  private static final Pattern FENCE =
      Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");

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
    List<String> chunks = new ArrayList<>();
    StringBuilder paragraph = new StringBuilder();
    StringBuilder code = new StringBuilder();
    String fence = null;

    for (String line : normalized.split("\n", -1)) {
      var marker = FENCE.matcher(line);
      if (fence != null) {
        code.append('\n').append(line);
        if (marker.matches()
            && marker.group(1).charAt(0) == fence.charAt(0)
            && marker.group(1).length() >= fence.length()
            && marker.group(2).isBlank()) {
          addCode(code, chunks);
          fence = null;
        }
      } else if (marker.matches()
          && (marker.group(1).charAt(0) != '`'
              || !marker.group(2).contains("`"))) {
        addParagraph(paragraph, chunks);
        fence = marker.group(1);
        code.append(line);
      } else if (line.isBlank()) {
        addParagraph(paragraph, chunks);
      } else {
        if (!paragraph.isEmpty()) {
          paragraph.append(' ');
        }
        paragraph.append(line);
      }
    }

    addParagraph(paragraph, chunks);
    addCode(code, chunks);
    return List.copyOf(chunks);
  }

  private void addParagraph(
      StringBuilder paragraph,
      List<String> chunks
  ) {
    String normalized = paragraph.toString()
        .replaceAll("(?U)\\s+", " ")
        .strip();
    paragraph.setLength(0);
    if (normalized.isEmpty()) {
      return;
    }
    if (normalized.length() <= maxCharacters) {
      chunks.add(normalized);
    } else {
      chunks.addAll(splitOversizedParagraph(normalized));
    }
  }

  private void addCode(
      StringBuilder code,
      List<String> chunks
  ) {
    // Slice without stripping: even blank lines and trailing spaces are code data.
    for (int start = 0; start < code.length(); start += maxCharacters) {
      chunks.add(code.substring(start, Math.min(start + maxCharacters, code.length())));
    }
    code.setLength(0);
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
        .replace('\r', '\n');
  }
}