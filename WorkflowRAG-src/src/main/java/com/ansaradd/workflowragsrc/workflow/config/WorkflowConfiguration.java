package com.ansaradd.workflowragsrc.workflow.config;

import com.ansaradd.workflowragsrc.workflow.model.PipelineTemplate;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfiguration {

  @Bean
  public PipelineTemplate defaultIngestionPipeline() {
    return new PipelineTemplate(
        "default-ingestion",
        List.of(
            "parse",
            "chunk",
            "embed",
            "index",
            "activate"
        )
    );
  }
}