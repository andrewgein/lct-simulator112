package com.simulator112.incident.application.service.generation.system112.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.SelectedFields;
import com.simulator112.incident.application.service.generation.StepPaths;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class System112CriteriaStep extends EditableJsonStep {
  public static final StepPaths PATHS = new StepPaths(Set.of("dialogueCriteria"),
      "Критерии 112: /dialogueCriteria или /dialogueCriteria/0/hypothesis (также name, weight).",
      (draft, parts) -> {
    if (parts.length == 1) return;
    StepPaths.existingIndex(parts[1], draft.path("dialogueCriteria"));
    if (parts.length > 3 || parts.length == 3 && !Set.of("name", "hypothesis", "weight").contains(parts[2]))
      throw new IllegalStateException("Invalid criteria patch path");
  });

  private final Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation;

  public System112CriteriaStep(
      ObjectMapper mapper,
      String scenario,
      JsonNode incident,
      String requests,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation) {
    this(mapper, scenario, incident, requests, validation, null);
  }

  public System112CriteriaStep(
      ObjectMapper mapper, String scenario, JsonNode incident, String requests,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation, Edit edit) {
    super("Критерии 112", editPrompt(messages(mapper, scenario, incident, requests), edit),
        "непустой dialogueCriteria с общей суммой весов не более 40", 3, mapper, edit);
    this.validation = validation;
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper, String scenario, JsonNode incident, String requests) {
    var criteriaMessages =
        List.of(
            new IncidentLanguageModelPort.Message(
                "system",
                """
Создай критерии оценки действий оператора Системы-112 для этого сценария.
Ответ только JSON: {"message":"Готово","incident":{"dialogueCriteria":
[{"name":"Уточнение адреса","hypothesis":"Оператор уточнил адрес происшествия","weight":10}]}}.
Верни 1–20 уместных критериев: name и hypothesis — непустые строки,
weight — целое от 1 до 40, сумма весов не более 40.
Критерии должны проверять действия оператора, а не знание моделью фактов.
Не возвращай другие поля incident.
"""
                    + "\nСитуация: "
                    + scenario
                    + "\nГотовые данные и этапы: "
                    + mapper.writeValueAsString(incident)
                    + "\nЗапросы пользователя: "
                    + requests));
    return criteriaMessages;
  }

  @Override
  protected JsonNode generate(JsonNode response) {
    var fields = response.path("incident");
    var items = fields.path("dialogueCriteria");
    if (!fields.isObject() || fields.size() != 1 || !items.isArray() || items.isEmpty())
      throw new IllegalStateException("Missing 112 dialogue criteria");
    return validation.apply(response).incident().path("dialogueCriteria").deepCopy();
  }

  @Override
  protected JsonNode update(JsonNode response) {
    var patch = SelectedFields.apply(mapper, edit.draft(), response.path("incident"), edit.paths());
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident();
  }
}
