//package com.ansaradd.workflowragsrc.retrieval;
//
//import com.ansaradd.workflowragsrc.embedding.provider.EmbeddingProvider;
//import java.util.List;
//import java.util.Locale;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Primary;
//
//public class RetrievalPipelineIntegrationTest {
//
//  @TestConfiguration
//  static class TestEmbeddingConfiguration {
//
//    @Bean
//    @Primary
//    EmbeddingProvider testEmbeddingProvider() {
//      return new TestEmbeddingProvider();
//    }
//  }
//
//  static class TestEmbeddingProvider
//      implements EmbeddingProvider {
//
//    private static final int DIMENSIONS =
//        1024;
//
//    private static final String REVISION =
//        "deterministic-revision-v1";
//
//    @Override
//    public String id() {
//      return "test";
//    }
//
//    @Override
//    public String model() {
//      return "deterministic-v1";
//    }
//
//    @Override
//    public String revision() {
//      return REVISION;
//    }
//
//    @Override
//    public int dimensions() {
//      return DIMENSIONS;
//    }
//
//    @Override
//    public List<float[]> embed(
//        List<String> texts
//    ) {
//      return texts
//          .stream()
//          .map(this::embed)
//          .toList();
//    }
//
//    private float[] embed(
//        String text
//    ) {
//      float[] vector =
//          new float[DIMENSIONS];
//
//      String normalized =
//          text.toLowerCase(
//              Locale.ROOT
//          );
//
//      if (normalized.contains(
//          "transaction"
//      )) {
//        vector[0] = 1.0f;
//      }
//
//      if (normalized.contains(
//          "docker"
//      )) {
//        vector[1] = 1.0f;
//      }
//
//      if (normalized.contains(
//          "spring"
//      )) {
//        vector[2] = 1.0f;
//      }
//
//      return vector;
//    }
//  }
//}