package com.simulator112.incident.application.service.generation.system112.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.JsonResponseStep;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class System112ScenarioStep extends JsonResponseStep<String> {
  @Override protected String status() { return "Генерирую сценарий"; }
  public System112ScenarioStep(
      ObjectMapper mapper,
      JsonNode modelDraft,
      List<GenerateIncidentDraftUseCase.Message> userMessages) {
    super(
        "Описание 112",
        messages(mapper, modelDraft, userMessages),
        "JSON с непустым scenario",
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
            Опиши СУТЬ нового учебного сценария Системы-112. Ответ только JSON:
            {"scenario":"краткое связное описание происшествия"}.
            Что известно к моменту обращения: происшествие, место, кто сообщает,
            пострадавшие и наблюдаемые угрозы. Не выдумывай причину, исход и будущие события,
            если пользователь их не задал. Не добавляй другие службы и звонки без запроса.
            Пока не создавай структурированные данные, этапы, звонки и критерии.
            Описание — 2–4 коротких предложения, не длиннее 1200 символов.
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
