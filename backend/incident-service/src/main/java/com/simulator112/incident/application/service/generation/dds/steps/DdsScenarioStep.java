package com.simulator112.incident.application.service.generation.dds.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.JsonResponseStep;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DdsScenarioStep extends JsonResponseStep<String> {
  @Override protected String status() { return "Генерирую сценарий"; }
  public DdsScenarioStep(
      ObjectMapper mapper,
      JsonNode modelDraft,
      List<GenerateIncidentDraftUseCase.Message> userMessages) {
    super(
        "Описание сценария",
        messages(mapper, modelDraft, userMessages),
        "JSON с непустым полем scenario",
        1,
        mapper);
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper,
      JsonNode modelDraft,
      List<GenerateIncidentDraftUseCase.Message> userMessages) {
    var scenarioMessages = new ArrayList<IncidentLanguageModelPort.Message>();
    scenarioMessages.add(
        new IncidentLanguageModelPort.Message(
            "system",
            """
            Опиши суть НОВОГО учебного сценария ДДС. Ответ только JSON:
            {"scenario":"краткое связное описание происшествия"}.
            Что произошло, где, кто сообщил, пострадавшие, хронология и работа бригады.
            Учитывай требования пользователя, не добавляй другие службы без запроса.
            Не составляй stagePlan, карточку, звонки и структурированные этапы.
            """
                + "\nЧерновик: "
                + mapper.writeValueAsString(modelDraft)));
    userMessages.forEach(
        message ->
            scenarioMessages.add(
                new IncidentLanguageModelPort.Message(message.role(), message.content())));
    return scenarioMessages;
  }

  @Override
  protected String validateJson(JsonNode response) {
    var description = response.path("scenario");
    if (!description.isTextual()
        || description.asText().isBlank()
        || description.asText().length() > 1200)
      throw new IllegalStateException("Expected concise scenario description");
    return description.asText();
  }
}
