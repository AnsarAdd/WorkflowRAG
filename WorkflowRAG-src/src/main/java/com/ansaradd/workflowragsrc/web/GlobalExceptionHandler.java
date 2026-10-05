package com.ansaradd.workflowragsrc.web;

import com.ansaradd.workflowragapi.model.response.ApiErrorResponse;
import com.ansaradd.workflowragsrc.document.exception.DocumentNotFoundException;
import com.ansaradd.workflowragsrc.document.exception.DocumentVersionBuildInProgressException;
import com.ansaradd.workflowragsrc.document.exception.DocumentVersionNotFoundException;
import com.ansaradd.workflowragsrc.document.exception.InvalidDocumentVersionStateException;
import com.ansaradd.workflowragsrc.document.exception.UnsupportedDocumentFormatException;
import com.ansaradd.workflowragsrc.source.exception.UnknownSourceException;
import com.ansaradd.workflowragsrc.workflow.exception.JobNotFoundException;
import com.ansaradd.workflowragsrc.workflow.exception.PipelineNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import com.ansaradd.workflowragsrc.workflow.exception.InvalidJobStateException;
import com.ansaradd.workflowragsrc.workflow.exception.WorkflowExecutionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log =
      LoggerFactory.getLogger(
          GlobalExceptionHandler.class
      );

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(UnknownSourceException.class)
  public ResponseEntity<ApiErrorResponse> handleUnknownSource(
      UnknownSourceException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.NOT_FOUND,
        "SOURCE_NOT_FOUND",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler({
      DocumentNotFoundException.class,
      DocumentVersionNotFoundException.class
  })
  public ResponseEntity<ApiErrorResponse> handleDocumentNotFound(
      RuntimeException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.NOT_FOUND,
        "DOCUMENT_NOT_FOUND",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(DocumentVersionBuildInProgressException.class)
  public ResponseEntity<ApiErrorResponse> handleBuildInProgress(
      DocumentVersionBuildInProgressException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.CONFLICT,
        "DOCUMENT_VERSION_BUILD_IN_PROGRESS",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(InvalidDocumentVersionStateException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidVersionState(
      InvalidDocumentVersionStateException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.CONFLICT,
        "INVALID_DOCUMENT_VERSION_STATE",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(UnsupportedDocumentFormatException.class)
  public ResponseEntity<ApiErrorResponse> handleUnsupportedFormat(
      UnsupportedDocumentFormatException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "UNSUPPORTED_DOCUMENT_FORMAT",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(JobNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleJobNotFound(
      JobNotFoundException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.NOT_FOUND,
        "JOB_NOT_FOUND",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(PipelineNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handlePipelineNotFound(
      PipelineNotFoundException exception,
      HttpServletRequest request
  ) {
    return error(
        HttpStatus.NOT_FOUND,
        "PIPELINE_NOT_FOUND",
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(InvalidJobStateException.class)
  public ResponseEntity<ApiErrorResponse> handleJobConflict(
      InvalidJobStateException exception, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "INVALID_JOB_STATE", exception.getMessage(), request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request body", request);
  }

  @ExceptionHandler(WorkflowExecutionException.class)
  public ResponseEntity<ApiErrorResponse> handleWorkflowFailure(
      WorkflowExecutionException exception, HttpServletRequest request) {
    log.error("Workflow failed", exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "WORKFLOW_FAILED", exception.getMessage(), request);
  }

  @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidParameter(
      RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request parameter", request);
  }

  @ExceptionHandler(com.ansaradd.workflowragsrc.reranking.exception.RerankingUnavailableException.class)
  public ResponseEntity<ApiErrorResponse> handleRerankingUnavailable(
      RuntimeException exception, HttpServletRequest request) {
    return error(HttpStatus.SERVICE_UNAVAILABLE, "RERANKING_UNAVAILABLE", exception.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception,
      HttpServletRequest request
  ) {
    log.error(
        "Unexpected error while processing {}",
        request.getRequestURI(),
        exception
    );

    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Unexpected internal error",
        request
    );
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request
  ) {
    ApiErrorResponse response =
        new ApiErrorResponse(
            code,
            message,
            request.getRequestURI(),
            Instant.now()
        );

    return ResponseEntity
        .status(status)
        .body(response);
  }
}