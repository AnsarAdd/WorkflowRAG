package com.ansaradd.workflowragapi.controller;

import static com.ansaradd.workflowragapi.constant.ApiConstant.INGESTIONS_URL;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.IngestionPreviewResponse;
import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping(INGESTIONS_URL + "/previews")
public interface IngestionPreviewController {
  @PostMapping("/source")
  IngestionPreviewResponse previewSource(@RequestBody SourceIngestionRequest request);

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  IngestionPreviewResponse previewUpload(
      @RequestParam String sourceId,
      @RequestParam String externalDocumentId,
      @RequestParam MultipartFile file
  ) throws IOException;

  @GetMapping("/{id}")
  IngestionPreviewResponse find(@PathVariable UUID id);

  @PostMapping("/{id}/confirm")
  SourceIngestionResponse confirm(@PathVariable UUID id);
}
