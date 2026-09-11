package com.ansaradd.workflowragsrc.source.service.impl;

import com.ansaradd.workflowragsrc.source.service.ContentHashService;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class Sha256ContentHashService implements ContentHashService {

  @Override
  public String calculate(byte[] content) {
    try {
      MessageDigest digest =
          MessageDigest.getInstance("SHA-256");

      return HexFormat.of()
          .formatHex(digest.digest(content));

    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
          "SHA-256 is not available",
          e
      );
    }
  }
}