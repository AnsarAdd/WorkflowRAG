package com.ansaradd.workflowragsrc.embedding.provider.ollama;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OllamaModelRevisionResolver {

  private final ObjectMapper objectMapper;

  public OllamaModelRevisionResolver(
      ObjectMapper objectMapper
  ) {
    this.objectMapper =
        objectMapper;
  }

  public String resolve(
      String baseUrl,
      String model,
      Duration timeout
  ) {
    Objects.requireNonNull(
        baseUrl,
        "baseUrl must not be null"
    );
    Objects.requireNonNull(
        model,
        "model must not be null"
    );
    Objects.requireNonNull(
        timeout,
        "timeout must not be null"
    );

    try {
      HttpClient httpClient =
          HttpClient
              .newBuilder()
              .connectTimeout(
                  timeout
              )
              .build();

      HttpRequest request =
          HttpRequest
              .newBuilder()
              .uri(
                  URI.create(
                      normalizeBaseUrl(
                          baseUrl
                      )
                          + "/api/tags"
                  )
              )
              .timeout(
                  timeout
              )
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(
              request,
              HttpResponse
                  .BodyHandlers
                  .ofString()
          );

      if (response.statusCode()
          != 200) {

        throw new IllegalStateException(
            "Failed to resolve Ollama model revision: HTTP "
                + response.statusCode()
        );
      }

      JsonNode models =
          objectMapper
              .readTree(
                  response.body()
              )
              .path(
                  "models"
              );

      for (JsonNode candidate
          : models) {

        String candidateName =
            candidate
                .path("name")
                .asText();

        String candidateModel =
            candidate
                .path("model")
                .asText();

        if (!matches(
            model,
            candidateName
        ) && !matches(
            model,
            candidateModel
        )) {
          continue;
        }

        String digest =
            candidate
                .path("digest")
                .asText();

        if (digest.isBlank()) {
          throw new IllegalStateException(
              "Ollama model has no digest: "
                  + model
          );
        }

        return digest;
      }

      throw new IllegalStateException(
          "Ollama model not found: "
              + model
      );

    } catch (InterruptedException exception) {
      Thread
          .currentThread()
          .interrupt();

      throw new IllegalStateException(
          "Interrupted while resolving Ollama model revision: "
              + model,
          exception
      );

    } catch (IllegalStateException exception) {
      throw exception;

    } catch (Exception exception) {
      throw new IllegalStateException(
          "Failed to resolve Ollama model revision: "
              + model,
          exception
      );
    }
  }

  private boolean matches(
      String configured,
      String actual
  ) {
    if (configured.equals(actual)) {
      return true;
    }

    return !configured.contains(":")
        && (configured + ":latest")
        .equals(actual);
  }

  private String normalizeBaseUrl(
      String baseUrl
  ) {
    String normalized =
        baseUrl.strip();

    while (normalized.endsWith("/")) {
      normalized =
          normalized.substring(
              0,
              normalized.length() - 1
          );
    }

    return normalized;
  }
}