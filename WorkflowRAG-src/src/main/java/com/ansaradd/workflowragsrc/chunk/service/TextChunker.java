package com.ansaradd.workflowragsrc.chunk.service;

import java.util.List;

public interface TextChunker {

  List<String> chunk(String content);
}