package com.ansaradd.workflowragsrc.chunk.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import com.ansaradd.workflowragsrc.chunk.config.ChunkingProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParagraphTextChunkerTest {
  private final ParagraphTextChunker chunker = new ParagraphTextChunker(new ChunkingProperties(100));

  @Test
  void insertionMovementAndLocalEditsLeaveOtherParagraphsIntact() {
    assertEquals(List.of("First paragraph.", "Second paragraph."),
        chunker.chunk("First paragraph.\n\nSecond paragraph."));
    assertEquals(List.of("Inserted.", "Second paragraph.", "First paragraph edited."),
        chunker.chunk("Inserted.\n\nSecond paragraph.\n\nFirst paragraph edited."));
  }

  @Test
  void normalizesProseWhitespaceWithinEachParagraph() {
    assertEquals(List.of("First line continues here.", "Next paragraph."),
        chunker.chunk("  First\tline\r\n continues   here.\r\n \t\r\nNext paragraph.  "));
  }

  @Test
  void preservesFencedCodeIncludingInternalBlankLinesAndIndentation() {
    String code = "```java\n  first();  \n\n\tsecond();\n```";
    assertEquals(List.of("Before code.", code, "After code."),
        chunker.chunk("Before code.\n" + code + "\nAfter code."));
    assertEquals(List.of("~~~\n  line\n\n~~~"), chunker.chunk("~~~\n  line\n\n~~~"));
  }

  @Test
  void boundsOversizedProseAndCodeWithoutLosingCodeWhitespace() {
    var small = new ParagraphTextChunker(new ChunkingProperties(12));
    assertEquals(List.of("Alpha beta", "gamma delta", "epsilon."),
        small.chunk("Alpha beta gamma delta epsilon."));
    String code = "```\n    first();  \n\n\tsecond();\n```";
    List<String> chunks = small.chunk(code);
    assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 12));
    assertEquals(code, String.join("", chunks));
  }
}
