package com.ansaradd.workflowragsrc.preflight.service;

import com.ansaradd.workflowragsrc.preflight.model.PreviewConfirmation;
import java.util.UUID;

public interface PreviewConfirmationService {
  PreviewConfirmation start(UUID id);
}
