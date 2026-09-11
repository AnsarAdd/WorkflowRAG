package com.ansaradd.workflowragapi.controller;

import static com.ansaradd.workflowragapi.constant.ApiConstant.INGESTIONS_URL;

import com.ansaradd.workflowragapi.model.request.SourceIngestionRequest;
import com.ansaradd.workflowragapi.model.response.SourceIngestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(INGESTIONS_URL)
public interface SourceIngestionController {

  @PostMapping("/source")
  ResponseEntity<SourceIngestionResponse> ingestSource(
      @Valid @RequestBody SourceIngestionRequest request);
}
