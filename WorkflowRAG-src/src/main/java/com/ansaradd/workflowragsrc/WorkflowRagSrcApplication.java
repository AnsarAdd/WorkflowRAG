package com.ansaradd.workflowragsrc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class WorkflowRagSrcApplication {

  public static void main(String[] args) {
    SpringApplication.run(WorkflowRagSrcApplication.class, args);
  }

}
