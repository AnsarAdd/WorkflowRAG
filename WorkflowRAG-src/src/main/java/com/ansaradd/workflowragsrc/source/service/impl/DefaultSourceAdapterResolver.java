package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragapi.model.enums.SourceType;
import com.ansaradd.workflowragsrc.source.adapter.SourceAdapter;
import com.ansaradd.workflowragsrc.source.exception.SourceAdapterNotFoundException;
import com.ansaradd.workflowragsrc.source.service.SourceAdapterResolver;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultSourceAdapterResolver
    implements SourceAdapterResolver {

  private final Map<SourceType, SourceAdapter> adapters;

  public DefaultSourceAdapterResolver(
      List<SourceAdapter> sourceAdapters
  ) {
    Map<SourceType, SourceAdapter> resolvedAdapters =
        new EnumMap<>(SourceType.class);

    for (SourceAdapter adapter : sourceAdapters) {

      SourceAdapter previous =
          resolvedAdapters.putIfAbsent(
              adapter.sourceType(),
              adapter
          );

      if (previous != null) {
        throw new IllegalStateException(
            "Multiple SourceAdapters registered for type: "
                + adapter.sourceType()
        );
      }
    }

    this.adapters = Map.copyOf(resolvedAdapters);
  }

  @Override
  public SourceAdapter resolve(SourceType sourceType) {
    SourceAdapter adapter = adapters.get(sourceType);

    if (adapter == null) {
      throw new SourceAdapterNotFoundException(sourceType);
    }

    return adapter;
  }
}