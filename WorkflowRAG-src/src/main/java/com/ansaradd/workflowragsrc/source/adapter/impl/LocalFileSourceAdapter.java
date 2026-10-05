package com.ansaradd.workflowragsrc.source.adapter.impl;

import com.ansaradd.workflowragapi.model.enums.DocumentFormat;
import com.ansaradd.workflowragapi.model.enums.SourceType;
import com.ansaradd.workflowragsrc.source.adapter.SourceAdapter;
import com.ansaradd.workflowragsrc.source.model.DocumentKey;
import com.ansaradd.workflowragsrc.source.exception.SourceLoadException;
import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.model.SourceLoadContext;
import java.io.File;
import com.ansaradd.workflowragsrc.preflight.config.PreflightProperties;
import com.ansaradd.workflowragsrc.preflight.exception.DocumentContractException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LocalFileSourceAdapter implements SourceAdapter {

  private final PreflightProperties limits;

  public LocalFileSourceAdapter(PreflightProperties limits) {
    this.limits = limits;
  }

  private static final String ROOT_CONFIGURATION_KEY = "root";

  @Override
  public SourceType sourceType() {
    return SourceType.LOCAL_FILE;
  }

  @Override
  public LoadedDocument load(SourceLoadContext context) {

    Source source = context.source();

    Path root = resolveRoot(source);

    Path file = resolveFile(
        root,
        context.externalDocumentId()
    );

    DocumentFormat format =
        detectFormat(file);

    try {
      if (Files.size(file) > limits.maxBytes()) {
        throw new DocumentContractException("Document exceeds size limit");
      }
      byte[] content;
      try (var input = Files.newInputStream(file)) {
        content = input.readNBytes(limits.maxBytes() + 1);
      }
      if (content.length > limits.maxBytes()) {
        throw new DocumentContractException("Document exceeds size limit");
      }

      String canonicalExternalDocumentId =
          toCanonicalExternalDocumentId(root, file);

      Map<String, Object> metadata =
          readMetadata(file);

      return new LoadedDocument(
          new DocumentKey(
              source.id(),
              canonicalExternalDocumentId
          ),
          format,
          content,
          metadata
      );

    } catch (IOException e) {
      throw new SourceLoadException(
          "Failed to read document '"
              + context.externalDocumentId()
              + "' from source '"
              + source.id()
              + "'",
          e
      );
    }
  }

  private Path resolveRoot(Source source) {

    Object configuredRoot =
        source.configuration()
            .get(ROOT_CONFIGURATION_KEY);

    if (!(configuredRoot instanceof String rootValue)
        || rootValue.isBlank()) {

      throw new SourceLoadException(
          "LOCAL_FILE source '"
              + source.id()
              + "' requires configuration property '"
              + ROOT_CONFIGURATION_KEY
              + "'"
      );
    }

    try {
      Path root = Path.of(rootValue)
          .toAbsolutePath()
          .normalize()
          .toRealPath();

      if (!Files.isDirectory(root)) {
        throw new SourceLoadException(
            "Configured root is not a directory for source '"
                + source.id()
                + "': "
                + root
        );
      }

      return root;

    } catch (IOException | InvalidPathException e) {
      throw new SourceLoadException(
          "Invalid LOCAL_FILE root for source '"
              + source.id()
              + "': "
              + rootValue,
          e
      );
    }
  }

  private Path resolveFile(
      Path root,
      String externalDocumentId
  ) {

    try {
      String normalizedId =
          externalDocumentId.replace('\\', '/');

      Path relativePath = Path.of(normalizedId);

      if (relativePath.isAbsolute()) {
        throw new SourceLoadException(
            "externalDocumentId must be relative: "
                + externalDocumentId
        );
      }

      Path candidate = root
          .resolve(relativePath)
          .normalize();

      /*
       * Protect against ../../ traversal before accessing filesystem.
       */
      if (!candidate.startsWith(root)) {
        throw new SourceLoadException(
            "Document escapes source root: "
                + externalDocumentId
        );
      }

      /*
       * Resolve symlinks.
       */
      Path realFile = candidate.toRealPath();

      /*
       * Protect against symlink escaping configured root.
       */
      if (!realFile.startsWith(root)) {
        throw new SourceLoadException(
            "Document resolves outside source root: "
                + externalDocumentId
        );
      }

      if (!Files.isRegularFile(realFile)) {
        throw new SourceLoadException(
            "Document is not a regular file: "
                + externalDocumentId
        );
      }

      return realFile;

    } catch (InvalidPathException e) {
      throw new SourceLoadException(
          "Invalid externalDocumentId: "
              + externalDocumentId,
          e
      );

    } catch (IOException e) {
      throw new SourceLoadException(
          "Document not found or cannot be resolved: "
              + externalDocumentId,
          e
      );
    }
  }

  private String toCanonicalExternalDocumentId(
      Path root,
      Path file
  ) {

    return root.relativize(file)
        .toString()
        .replace(
            File.separatorChar,
            '/'
        );
  }

  private DocumentFormat detectFormat(Path file) {

    String fileName = file.getFileName()
        .toString()
        .toLowerCase(Locale.ROOT);

    int extensionIndex =
        fileName.lastIndexOf('.');

    if (extensionIndex < 0) {
      throw unsupportedFormat(fileName);
    }

    String extension =
        fileName.substring(extensionIndex + 1);

    return switch (extension) {

      case "txt" ->
          DocumentFormat.TEXT;

      case "md", "markdown" ->
          DocumentFormat.MARKDOWN;

      case "html", "htm" ->
          DocumentFormat.HTML;

      case "pdf" ->
          DocumentFormat.PDF;

      case "docx" ->
          DocumentFormat.DOCX;

      case "java",
           "kt",
           "kts",
           "py",
           "js",
           "ts",
           "cs",
           "go",
           "rs" ->
          DocumentFormat.SOURCE_CODE;

      default ->
          throw unsupportedFormat(fileName);
    };
  }

  private SourceLoadException unsupportedFormat(
      String fileName
  ) {

    return new SourceLoadException(
        "Unsupported document format: "
            + fileName
    );
  }

  private Map<String, Object> readMetadata(
      Path file
  ) throws IOException {

    Map<String, Object> metadata =
        new HashMap<>();

    metadata.put(
        "fileName",
        file.getFileName().toString()
    );

    metadata.put(
        "size",
        Files.size(file)
    );

    Instant lastModified =
        Files.getLastModifiedTime(file)
            .toInstant();

    metadata.put(
        "lastModified",
        lastModified.toString()
    );

    return Map.copyOf(metadata);
  }
}