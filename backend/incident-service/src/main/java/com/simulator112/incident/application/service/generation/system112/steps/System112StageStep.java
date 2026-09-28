package com.simulator112.incident.application.service.generation.system112.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.SelectedFields;
import com.simulator112.incident.application.service.generation.StepPaths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class System112StageStep extends EditableJsonStep {
  public static final StepPaths PATHS = new StepPaths(Set.of("stages"),
      "Этапы 112: /stages для перестройки всех этапов, /stages/0/title или "
          + "/stages/0/calls/0/knownFacts для правки одного этапа.",
      (draft, parts) -> {
    if (parts.length == 1) return;
    int index = StepPaths.existingIndex(parts[1], draft.path("stages"));
    if (parts.length == 2) return;
    if (!Set.of("title", "description", "classifierCodes", "victimCount", "calls").contains(parts[2]))
      throw new IllegalStateException("Invalid 112 stage patch path");
    if (parts.length == 3) return;
    if (!"calls".equals(parts[2]) || parts.length > 6)
      throw new IllegalStateException("Invalid call patch path");
    StepPaths.existingIndex(parts[3], draft.path("stages").get(index).path("calls"));
    if (parts.length == 4) return;
    if (!Set.of("direction", "counterparty", "person", "gender", "knownFacts",
        "hiddenFacts", "aiContext", "emotionalState").contains(parts[4]))
      throw new IllegalStateException("Invalid call patch field");
    if (parts.length == 5) return;
    if (!"person".equals(parts[4]) || !Set.of("firstName", "lastName", "middleName",
        "age", "phone", "contactPhone", "onScenePhone", "address", "additionalInfo").contains(parts[5]))
      throw new IllegalStateException("Invalid person patch field");
  });

  private final Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation;
  private final int targetIndex;

  public System112StageStep(
      ObjectMapper mapper,
      int index,
      JsonNode plan,
      ArrayNode stages,
      JsonNode incident,
      LinkedHashMap<String, String> codes,
      String scenario,
      String requests,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation) {
    this(mapper, index, plan, stages, incident, codes, scenario, requests, validation, null, -1);
  }

  public System112StageStep(
      ObjectMapper mapper, int index, JsonNode plan, ArrayNode stages, JsonNode incident,
      LinkedHashMap<String, String> codes, String scenario, String requests,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation, Edit edit, int targetIndex) {
    super(
        "Этап 112 " + (index + 1),
        editPrompt(messages(mapper, index, plan, stages, incident, codes, scenario, requests), edit),
        "один stage с кодом классификатора и звонком",
        3,
        mapper,
        edit);
    this.validation = validation;
    this.targetIndex = targetIndex;
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper,
      int index,
      JsonNode plan,
      ArrayNode stages,
      JsonNode incident,
      LinkedHashMap<String, String> codes,
      String scenario,
      String requests) {
    var goal = plan.get(index);
    var summaries = summaries(mapper, stages);
    var stageMessages =
        List.of(
            new IncidentLanguageModelPort.Message(
                "system",
                """
                Создай только ОДИН этап учебного сценария Системы-112.
                Ответ только JSON: {"stage":{"title":"...","description":"...",
                "classifierCodes":["код"],"victimCount":0,"calls":[{"direction":"INBOUND",
                "counterparty":"CALLER","person":{"firstName":"Иван","lastName":"Петров",
                "phone":"+79990000000"},"knownFacts":["Известный факт"],"hiddenFacts":[],
                "gender":"MAN","emotionalState":"WORRIED","aiContext":"Отвечает встревоженно"}]}}.
                Для звонка direction только INBOUND, counterparty только CALLER.
                gender ТОЛЬКО MAN или WOMEN (не MALE/FEMALE), поле звонка, НЕ person.
                emotionalState и aiContext также относятся к звонку, а не к person.
                Обязательно один или несколько звонков с придуманным человеком, именем, фамилией,
                телефоном и известными фактами. Коды происшествия выбирай только из списка.
                Не выдумывай id и position. Сохраняй факты, адрес, число пострадавших и хронологию.
                Не повторяй уже принятые этапы. Верни только этот этап, не весь сценарий.
                """
                    + "\nСитуация: "
                    + scenario
                    + "\nОсновные данные: "
                    + mapper.writeValueAsString(incident)
                    + "\nКоды классификатора: "
                    + mapper.writeValueAsString(codes)
                    + "\nЗапросы пользователя: "
                    + requests
                    + "\nПлан: "
                    + mapper.writeValueAsString(plan)
                    + "\nТекущий этап "
                    + (index + 1)
                    + " из "
                    + plan.size()
                    + ": "
                    + mapper.writeValueAsString(goal)
                    + "\nПредыдущие принятые этапы: "
                    + mapper.writeValueAsString(summaries)));
    return stageMessages;
  }

  @Override
  protected JsonNode generate(JsonNode response) {
    if (!response.isObject()
        || response.size() != 1
        || !(response.path("stage") instanceof ObjectNode stage)
        || !stage.path("title").isTextual()
        || stage.path("title").asText().isBlank())
      throw new IllegalStateException("Expected one 112 stage with title");
    stage.remove(List.of("id", "position"));
    for (var call : stage.path("calls"))
      if (call instanceof ObjectNode object) object.remove(List.of("id", "position"));
    var patch = mapper.createObjectNode();
    patch.set("stages", mapper.createArrayNode().add(stage));
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident().path("stages").get(0);
  }

  @Override
  protected JsonNode update(JsonNode response) {
    var stage = response.path("stage");
    if (!stage.isObject()) throw new IllegalStateException("Expected one 112 stage");
    var stages = (ArrayNode) edit.draft().path("stages").deepCopy();
    stages.set(targetIndex, stage.deepCopy());
    var incident = mapper.createObjectNode();
    incident.set("stages", stages);
    var patch = SelectedFields.apply(mapper, edit.draft(), incident, edit.paths());
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident();
  }

  public static JsonNode updateTimeline(ObjectMapper mapper, JsonNode draft, ArrayNode stages) {
    var incident = mapper.createObjectNode();
    incident.set("stages", stages);
    return SelectedFields.apply(mapper, draft, incident, List.of("/stages"));
  }

  private static ArrayNode summaries(ObjectMapper mapper, ArrayNode stages) {
    var summaries = mapper.createArrayNode();
    for (var previous : stages) {
      var summary = mapper.createObjectNode().put("title", previous.path("title").asText());
      summary.set("classifierCodes", previous.path("classifierCodes").deepCopy());
      var facts = mapper.createArrayNode();
      for (var call : previous.path("calls"))
        for (var fact : call.path("knownFacts")) if (facts.size() < 6) facts.add(fact.deepCopy());
      summary.set("knownFacts", facts);
      summaries.add(summary);
    }
    return summaries;
  }
}
