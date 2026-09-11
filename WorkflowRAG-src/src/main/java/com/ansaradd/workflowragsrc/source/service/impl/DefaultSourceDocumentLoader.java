package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragsrc.source.adapter.SourceAdapter;
import com.ansaradd.workflowragsrc.source.model.LoadedDocument;
import com.ansaradd.workflowragsrc.source.model.Source;
import com.ansaradd.workflowragsrc.source.model.SourceLoadContext;
import com.ansaradd.workflowragsrc.source.service.SourceAdapterResolver;
import com.ansaradd.workflowragsrc.source.service.SourceDocumentLoader;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultSourceDocumentLoader implements SourceDocumentLoader {

  private final SourceAdapterResolver sourceAdapterResolver;

  public DefaultSourceDocumentLoader(
      SourceAdapterResolver sourceAdapterResolver
  ) {
    this.sourceAdapterResolver = sourceAdapterResolver;
  }

  @Override
  public LoadedDocument load(
      Source source,
      String externalDocumentId,
      Map<String, String> parameters
  ) {
    Objects.requireNonNull(source, "source must not be null");

    SourceAdapter adapter =
        sourceAdapterResolver.resolve(source.type());

    SourceLoadContext context =
        new SourceLoadContext(
            source,
            externalDocumentId,
            parameters
        );

    return adapter.load(context);
  }
}