package com.ansaradd.workflowragsrc.workflow.service;

import com.ansaradd.workflowragsrc.workflow.model.JobStep;
import java.util.UUID;

public interface JobStepLifecycleService {

  JobStep start(UUID stepId);

  void succeed(
      UUID stepId,
      String fingerprint,
      String outputReference
  );

  void fail(
      UUID stepId,
      String error
  );

  void skip(
      UUID stepId,
      String fingerprint,
      String outputReference
  );
}