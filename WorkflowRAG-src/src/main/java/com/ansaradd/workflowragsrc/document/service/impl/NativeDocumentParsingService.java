package com.ansaradd.workflowragsrc.document.service.impl;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragsrc.document.exception.UnsupportedDocumentFormatException;
import com.ansaradd.workflowragsrc.document.service.DocumentParsingService;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.parser.model.ParsedSection;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class NativeDocumentParsingService
    implements DocumentParsingService {

  private static final String ROOT_KEY = "root";

  private static final Pattern ATX_HEADING =
      Pattern.compile("^\\s{0,3}(#{1,6})(?:\\s+|$)(.*)$");

  private static final Pattern CLOSING_HASHES =
      Pattern.compile("\\s+#+\\s*$");

  @Override
  public ParsedDocument parse(
      DocumentFormat format,
      byte[] content
  ) {
    Objects.requireNonNull(format, "format must not be null");
    Objects.requireNonNull(content, "content must not be null");

    String text = decode(content);

    return switch (format) {
      case TEXT -> parseText(text);
      case MARKDOWN -> parseMarkdown(text);
      default -> throw new UnsupportedDocumentFormatException(format);
    };
  }

  private ParsedDocument parseText(String text) {
    return new ParsedDocument(
        List.of(
            new ParsedSection(
                ROOT_KEY,
                null,
                null,
                0,
                text
            )
        )
    );
  }

  private ParsedDocument parseMarkdown(String text) {
    List<ParsedSection> sections = new ArrayList<>();

    Map<Integer, String> parentByLevel = new HashMap<>();
    Map<String, Integer> stableKeyOccurrences = new HashMap<>();
    stableKeyOccurrences.put(ROOT_KEY, 1);

    String currentStableKey = ROOT_KEY;
    String currentParentStableKey = null;
    String currentTitle = null;
    int currentLevel = 0;

    List<String> currentContent = new ArrayList<>();

    int activeFence = 0;

    String[] lines = text.split("\n", -1);

    for (String line : lines) {
      if (activeFence != 0) {
        currentContent.add(line);

        if (isClosingFence(line, activeFence)) {
          activeFence = 0;
        }

        continue;
      }

      int openingFence = detectOpeningFence(line);

      if (openingFence != 0) {
        activeFence = openingFence;
        currentContent.add(line);
        continue;
      }

      Matcher headingMatcher =
          ATX_HEADING.matcher(line);

      if (!headingMatcher.matches()) {
        currentContent.add(line);
        continue;
      }

      sections.add(
          new ParsedSection(
              currentStableKey,
              currentParentStableKey,
              currentTitle,
              currentLevel,
              normalizeContent(currentContent)
          )
      );

      int headingLevel =
          headingMatcher.group(1).length();

      String headingTitle =
          normalizeHeadingTitle(
              headingMatcher.group(2)
          );

      String parentStableKey =
          findParentStableKey(
              parentByLevel,
              headingLevel
          );

      String stableKey =
          createStableKey(
              parentStableKey,
              headingTitle,
              stableKeyOccurrences
          );

      parentByLevel.keySet()
          .removeIf(level -> level >= headingLevel);

      parentByLevel.put(
          headingLevel,
          stableKey
      );

      currentStableKey = stableKey;
      currentParentStableKey = parentStableKey;
      currentTitle = headingTitle;
      currentLevel = headingLevel;

      currentContent = new ArrayList<>();
    }

    sections.add(
        new ParsedSection(
            currentStableKey,
            currentParentStableKey,
            currentTitle,
            currentLevel,
            normalizeContent(currentContent)
        )
    );

    return new ParsedDocument(
        List.copyOf(sections)
    );
  }

  private String findParentStableKey(
      Map<Integer, String> parentByLevel,
      int headingLevel
  ) {
    for (int level = headingLevel - 1;
        level >= 1;
        level--) {

      String parent = parentByLevel.get(level);

      if (parent != null) {
        return parent;
      }
    }

    return ROOT_KEY;
  }

  private String createStableKey(
      String parentStableKey,
      String title,
      Map<String, Integer> occurrences
  ) {
    String slug = slugify(title);

    String baseKey =
        ROOT_KEY.equals(parentStableKey)
            ? slug
            : parentStableKey + "/" + slug;

    int occurrence =
        occurrences.merge(
            baseKey,
            1,
            Integer::sum
        );

    if (occurrence == 1) {
      return baseKey;
    }

    return baseKey + "#" + occurrence;
  }

  private String slugify(String title) {
    String normalized =
        Normalizer.normalize(
                title,
                Normalizer.Form.NFKC
            )
            .toLowerCase(Locale.ROOT);

    String slug =
        normalized
            .replaceAll("[^\\p{L}\\p{N}]+", "-")
            .replaceAll("^-+|-+$", "");

    return slug.isBlank()
        ? "section"
        : slug;
  }

  private String normalizeHeadingTitle(String title) {
    String normalized =
        CLOSING_HASHES
            .matcher(title)
            .replaceFirst("")
            .strip();

    return normalized;
  }

  private String normalizeContent(List<String> lines) {
    int start = 0;
    int end = lines.size();

    while (start < end
        && lines.get(start).isBlank()) {
      start++;
    }

    while (end > start
        && lines.get(end - 1).isBlank()) {
      end--;
    }

    return String.join(
        "\n",
        lines.subList(start, end)
    );
  }

  private String decode(byte[] content) {
    String text =
        new String(
            content,
            StandardCharsets.UTF_8
        );

    if (!text.isEmpty()
        && text.charAt(0) == '\uFEFF') {
      text = text.substring(1);
    }

    return text
        .replace("\r\n", "\n")
        .replace('\r', '\n');
  }

  private int detectOpeningFence(String line) {
    int indentation = leadingSpaces(line);

    if (indentation > 3) {
      return 0;
    }

    String candidate =
        line.substring(indentation);

    if (candidate.isEmpty()) {
      return 0;
    }

    char marker = candidate.charAt(0);

    if (marker != '`' && marker != '~') {
      return 0;
    }

    int length = countLeading(
        candidate,
        marker
    );

    if (length < 3) {
      return 0;
    }

    return marker == '`'
        ? length
        : -length;
  }

  private boolean isClosingFence(
      String line,
      int activeFence
  ) {
    int indentation = leadingSpaces(line);

    if (indentation > 3) {
      return false;
    }

    String candidate =
        line.substring(indentation);

    char marker =
        activeFence > 0
            ? '`'
            : '~';

    int requiredLength =
        Math.abs(activeFence);

    int actualLength =
        countLeading(
            candidate,
            marker
        );

    if (actualLength < requiredLength) {
      return false;
    }

    return candidate
        .substring(actualLength)
        .isBlank();
  }

  private int countLeading(
      String value,
      char expected
  ) {
    int count = 0;

    while (count < value.length()
        && value.charAt(count) == expected) {
      count++;
    }

    return count;
  }

  private int leadingSpaces(String line) {
    int count = 0;

    while (count < line.length()
        && line.charAt(count) == ' ') {
      count++;
    }

    return count;
  }

}