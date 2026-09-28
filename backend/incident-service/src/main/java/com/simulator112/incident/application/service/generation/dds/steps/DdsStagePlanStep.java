package com.simulator112.incident.application.service.generation.dds.steps;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.JsonResponseStep;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DdsStagePlanStep extends JsonResponseStep<JsonNode> {
  @Override protected String status() { return "Генерирую план"; }
  public DdsStagePlanStep(ObjectMapper mapper, String scenario, String userRequirements) {
    super(
        "План этапов",
        messages(mapper, scenario, userRequirements),
        "stagePlan с кратким планом промежуточных этапов",
        3,
        mapper);
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper, String scenario, String userRequirements) {
    var planMessages = new ArrayList<IncidentLanguageModelPort.Message>();
    planMessages.add(
        new IncidentLanguageModelPort.Message(
            "system",
            """
            На основе описания составь ТОЛЬКО план промежуточных этапов ДДС. Ответ только JSON:
            {"stagePlan":[{"type":"CALL_BRIGADE_FOR_STATUS","goal":"Уточнить статус бригады"}]}.
            По умолчанию планируй 3–4 промежуточных этапа, включая звонок бригаде, если для сценария не требуется больше.
            Вместе с первым и завершающим этапами, которые добавит сервер, получится 5–6 этапов.
            Если пользователь явно запросил число промежуточных этапов, сохрани его. Каждый goal — краткая непустая цель.
            Допустимые type: WAIT_FOR_BRIGADE_STATUS_CHANGE, CALL_BRIGADE_FOR_STATUS,
            REQUEST_ADDITIONAL_SERVICE.
            Другие службы — только по запросу пользователя. Первый ASSIGN_BRIGADE и последний
            COMPLETE_INCIDENT добавит сервер. Не возвращай сам сценарий, карточку или этапы.
            """
                + "\nОписание: "
                + scenario
                + "\nЗапросы пользователя: "
                + userRequirements));
    return planMessages;
  }

  @Override
  protected JsonNode validateJson(JsonNode response) {
    var stages = response.path("stagePlan");
    if (stages.isMissingNode()) stages = response.path("incident").path("stagePlan");
    if (!stages.isArray() || stages.isEmpty() || stages.size() > 13)
      throw new IllegalStateException("stagePlan must contain 1–13 intermediate stages");
    for (var item : stages) {
      if (!item.isObject()
          || !List.of(
                  "WAIT_FOR_BRIGADE_STATUS_CHANGE",
                  "CALL_BRIGADE_FOR_STATUS",
                  "REQUEST_ADDITIONAL_SERVICE")
              .contains(item.path("type").asText())
          || !item.path("goal").isTextual()
          || item.path("goal").asText().isBlank()
          || item.path("goal").asText().length() > 200)
        throw new IllegalStateException("Invalid intermediate stage plan");
    }
    return stages.deepCopy();
  }
}
