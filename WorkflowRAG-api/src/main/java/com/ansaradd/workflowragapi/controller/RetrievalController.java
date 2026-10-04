package com.ansaradd.workflowragapi.controller;

import static com.ansaradd.workflowragapi.constant.ApiConstant.RETRIEVAL_URL;

import com.ansaradd.workflowragapi.model.request.RetrievalContextRequest;
import com.ansaradd.workflowragapi.model.request.RetrievalSearchRequest;
import com.ansaradd.workflowragapi.model.response.RetrievalContextResponse;
import com.ansaradd.workflowragapi.model.response.RetrievalSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(RETRIEVAL_URL)
public interface RetrievalController {

  @PostMapping("/search")
  ResponseEntity<RetrievalSearchResponse> search(
      @RequestBody RetrievalSearchRequest request
  );

  @PostMapping("/context")
  ResponseEntity<RetrievalContextResponse> context(
      @RequestBody RetrievalContextRequest request
  );
}