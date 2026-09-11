//package com.ansaradd.workflowragsrc.document.service.impl;
//
//import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
//import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
//import com.ansaradd.workflowragsrc.parser.model.ParsedSection;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class NativeDocumentParsingServiceTest {
//
//  private final NativeDocumentParsingService service =
//      new NativeDocumentParsingService();
//
//  @Test
//  void shouldParseTextAsSingleRootSection() {
//    ParsedDocument result =
//        service.parse(
//            DocumentFormat.TEXT,
//            "hello world".getBytes(StandardCharsets.UTF_8)
//        );
//
//    assertEquals(1, result.sections().size());
//
//    ParsedSection root = result.sections().getFirst();
//
//    assertEquals("root", root.stableKey());
//    assertEquals(null, root.parentStableKey());
//    assertEquals(null, root.title());
//    assertEquals(0, root.level());
//    assertEquals("hello world", root.content());
//  }
//
//  @Test
//  void shouldParseMarkdownHierarchy() {
//    String markdown = """
//        Intro
//
//        # Installation
//
//        Install text.
//
//        ## Docker
//
//        Docker text.
//
//        ## Local
//
//        Local text.
//
//        # Usage
//
//        Usage text.
//        """;
//
//    ParsedDocument result =
//        service.parse(
//            DocumentFormat.MARKDOWN,
//            markdown.getBytes(StandardCharsets.UTF_8)
//        );
//
//    List<ParsedSection> sections = result.sections();
//
//    assertEquals(5, sections.size());
//
//    assertSection(
//        sections.get(0),
//        "root",
//        null,
//        null,
//        0,
//        "Intro"
//    );
//
//    assertSection(
//        sections.get(1),
//        "installation",
//        "root",
//        "Installation",
//        1,
//        "Install text."
//    );
//
//    assertSection(
//        sections.get(2),
//        "installation/docker",
//        "installation",
//        "Docker",
//        2,
//        "Docker text."
//    );
//
//    assertSection(
//        sections.get(3),
//        "installation/local",
//        "installation",
//        "Local",
//        2,
//        "Local text."
//    );
//
//    assertSection(
//        sections.get(4),
//        "usage",
//        "root",
//        "Usage",
//        1,
//        "Usage text."
//    );
//  }
//
//  @Test
//  void shouldGenerateUniqueStableKeysForDuplicateHeadings() {
//    String markdown = """
//        # Installation
//
//        ## Example
//
//        first
//
//        ## Example
//
//        second
//        """;
//
//    ParsedDocument result =
//        service.parse(
//            DocumentFormat.MARKDOWN,
//            markdown.getBytes(StandardCharsets.UTF_8)
//        );
//
//    assertEquals(
//        "installation/example",
//        result.sections().get(2).stableKey()
//    );
//
//    assertEquals(
//        "installation/example#2",
//        result.sections().get(3).stableKey()
//    );
//  }
//
//  @Test
//  void shouldIgnoreHeadingsInsideCodeFence() {
//    String markdown = """
//        # Code
//
//        ```java
//        # not-a-heading
//        System.out.println("hello");
//        ```
//
//        ## Real
//
//        text
//        """;
//
//    ParsedDocument result =
//        service.parse(
//            DocumentFormat.MARKDOWN,
//            markdown.getBytes(StandardCharsets.UTF_8)
//        );
//
//    assertEquals(3, result.sections().size());
//
//    assertEquals(
//        "code",
//        result.sections().get(1).stableKey()
//    );
//
//    assertEquals(
//        "code/real",
//        result.sections().get(2).stableKey()
//    );
//  }
//
//  @Test
//  void shouldNotConflictWithReservedRootStableKey() {
//    String markdown = """
//        # Root
//
//        content
//        """;
//
//    ParsedDocument result =
//        service.parse(
//            DocumentFormat.MARKDOWN,
//            markdown.getBytes(StandardCharsets.UTF_8)
//        );
//
//    assertEquals(2, result.sections().size());
//
//    assertEquals(
//        "root",
//        result.sections().get(0).stableKey()
//    );
//
//    assertEquals(
//        "root#2",
//        result.sections().get(1).stableKey()
//    );
//  }
//
//  private void assertSection(
//      ParsedSection section,
//      String stableKey,
//      String parentStableKey,
//      String title,
//      int level,
//      String content
//  ) {
//    assertEquals(stableKey, section.stableKey());
//    assertEquals(parentStableKey, section.parentStableKey());
//    assertEquals(title, section.title());
//    assertEquals(level, section.level());
//    assertEquals(content, section.content());
//  }
//}