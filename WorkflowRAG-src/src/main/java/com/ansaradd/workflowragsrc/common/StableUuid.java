package com.ansaradd.workflowragsrc.common;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StableUuid {

  private StableUuid() {
  }

  public static UUID section(
      UUID documentVersionId,
      String stableKey
  ) {
    return UUID.nameUUIDFromBytes(
        (
            "section\n"
                + documentVersionId
                + "\n"
                + stableKey
        ).getBytes(StandardCharsets.UTF_8)
    );
  }

  public static UUID chunk(
      UUID sectionId,
      int chunkIndex
  ) {
    return UUID.nameUUIDFromBytes(
        (
            "chunk\n"
                + sectionId
                + "\n"
                + chunkIndex
        ).getBytes(StandardCharsets.UTF_8)
    );
  }
}