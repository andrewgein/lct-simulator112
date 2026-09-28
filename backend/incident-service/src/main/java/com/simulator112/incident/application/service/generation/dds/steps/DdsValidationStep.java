package com.simulator112.incident.application.service.generation.dds.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase.Command;
import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase.Result;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.service.GeneratedIncidentPatchValidator;
import java.util.LinkedHashMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DdsValidationStep {
  private final ClassifierCatalogPort classifierCatalog;
  private final GeneratedIncidentPatchValidator validator;
  private final ObjectMapper mapper;

  public DdsValidationStep(
      ClassifierCatalogPort classifierCatalog,
      GeneratedIncidentPatchValidator validator,
      ObjectMapper mapper) {
    this.classifierCatalog = classifierCatalog;
    this.validator = validator;
    this.mapper = mapper;
  }

  public Result validate(
      JsonNode response,
      LinkedHashMap<String, String> classifierNames,
      LinkedHashMap<String, String> services,
      Command command) {
    DdsPreparedCardStep.prepareForValidation(response);
    DdsIntermediateStageStep.prepareForValidation(mapper, response, command.draft());
    var result = validator.validate(
        mapper.writeValueAsString(response), classifierNames.keySet(), services.keySet(), true, command.draft());
    DdsPreparedCardStep.validateServices(result, classifierCatalog, mapper);
    return DdsIntermediateStageStep.validateServices(result, classifierCatalog);
  }
}
