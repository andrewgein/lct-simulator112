package com.simulator112.incident.application.service.generation.system112.steps;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.JsonResponseStep;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class System112StagePlanStep extends JsonResponseStep<JsonNode> {
  @Override protected String status() { return "Генерирую план"; }
  public System112StagePlanStep(ObjectMapper mapper, String scenario, String requests) {
    super("План 112", messages(mapper, scenario, requests), "stagePlan с 1–15 целями", 3, mapper);
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper, String scenario, String requests) {
    var planMessages =
        List.of(
            new IncidentLanguageModelPort.Message(
                "system",
                """
Запланируй промежуточные этапы учебного сценария Системы-112.
Ответ только JSON: {"stagePlan":[{"goal":"Получить первое сообщение о происшествии"}]}.
Верни от 1 до 15 этапов, по умолчанию ОДИН этап с одним входящим звонком.
Явно запрошенное число этапов сохрани. У каждого этапа короткая непустая цель.
Не превращай вопросы оператору, проверку фактов и отдельные действия в новые этапы.
Несколько звонков могут быть внутри ОДНОГО этапа.
Не создавай поля, звонки и критерии на этом шаге.
"""
                    + "\nСитуация: "
                    + scenario
                    + "\nЗапросы пользователя: "
                    + requests));
    return planMessages;
  }

  @Override
  protected JsonNode validateJson(JsonNode response) {
    var stages = response.path("stagePlan");
    if (!stages.isArray() || stages.isEmpty() || stages.size() > 15)
      throw new IllegalStateException("112 stagePlan must contain 1–15 stages");
    for (var item : stages)
      if (!item.isObject()
          || !item.path("goal").isTextual()
          || item.path("goal").asText().isBlank()
          || item.path("goal").asText().length() > 200)
        throw new IllegalStateException("Invalid 112 stage goal");
    return stages.deepCopy();
  }
}
