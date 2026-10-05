package com.ansaradd.workflowragsrc.preflight.service.impl;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragsrc.chunk.service.TextChunker;
import com.ansaradd.workflowragsrc.document.service.DocumentParsingService;
import com.ansaradd.workflowragsrc.parser.model.ParsedDocument;
import com.ansaradd.workflowragsrc.preflight.config.PreflightProperties;
import com.ansaradd.workflowragsrc.preflight.exception.DocumentContractException;
import com.ansaradd.workflowragsrc.preflight.service.DocumentContractValidator;
import com.ansaradd.workflowragsrc.source.model.PreparedDocument;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentContractValidator implements DocumentContractValidator {
  private final PreflightProperties properties;
  private final DocumentParsingService parsingService;
  private final TextChunker textChunker;

  public DefaultDocumentContractValidator(
      PreflightProperties properties,
      DocumentParsingService parsingService,
      TextChunker textChunker
  ) {
    this.properties = properties;
    this.parsingService = parsingService;
    this.textChunker = textChunker;
  }

  @Override
  public ParsedDocument validate(PreparedDocument document) {
    if (document.format() != DocumentFormat.TEXT && document.format() != DocumentFormat.MARKDOWN) {
      throw new DocumentContractException("Only TXT and Markdown documents are supported");
    }
    byte[] bytes = document.content();
    if (bytes.length == 0 || bytes.length > properties.maxBytes()) {
      throw new DocumentContractException("Document size must be between 1 and " + properties.maxBytes() + " bytes");
    }
    try {
      String text = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes)).toString();
      if (text.indexOf('\0') >= 0) {
        throw new DocumentContractException("Document contains binary NUL characters");
      }
    } catch (CharacterCodingException exception) {
      throw new DocumentContractException("Document must contain valid UTF-8 text");
    }
    ParsedDocument parsed = parsingService.parse(document.format(), bytes);
    if (parsed.sections().size() > properties.maxSections()) {
      throw new DocumentContractException("Document has too many sections");
    }
    int chunks = 0;
    for (var section : parsed.sections()) {
      chunks += textChunker.chunk(section.content()).size();
      if (chunks > properties.maxChunks()) {
        throw new DocumentContractException("Document has too many chunks");
      }
    }
    if (chunks == 0) {
      throw new DocumentContractException("Document has no indexable text");
    }
    return parsed;
  }
}
