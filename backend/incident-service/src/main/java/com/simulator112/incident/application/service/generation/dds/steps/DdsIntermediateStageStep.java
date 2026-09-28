package com.simulator112.incident.application.service.generation.dds.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.SelectedFields;
import com.simulator112.incident.application.service.generation.StepPaths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class DdsIntermediateStageStep extends EditableJsonStep {
  public static final StepPaths PATHS = new StepPaths(Set.of("stages"),
      "Этапы ДДС: /stages для перестройки всех этапов, /stages/1/title или "
          + "/stages/1/calls/0/knownFacts для правки промежуточного этапа. "
          + "Граничные этапы ДДС нельзя менять по отдельности.",
      (draft, parts) -> {
    if (parts.length == 1) return;
    int index = StepPaths.existingIndex(parts[1], draft.path("stages"));
    if (index == 0 || index == draft.path("stages").size() - 1)
      throw new IllegalStateException("DDS boundary stages cannot be edited");
    if (parts.length == 2) return;
    if (!Set.of("title", "description", "type", "timeLimitSeconds", "actualStatus",
        "expectedComment", "calls").contains(parts[2]))
      throw new IllegalStateException("Invalid DDS stage patch path");
    if (parts.length == 3) return;
    if (!"calls".equals(parts[2]) || parts.length > 6)
      throw new IllegalStateException("Invalid call patch path");
    StepPaths.existingIndex(parts[3], draft.path("stages").get(index).path("calls"));
    if (parts.length == 4) return;
    if (!Set.of("direction", "counterparty", "serviceCode", "person", "gender",
        "knownFacts", "hiddenFacts", "aiContext", "emotionalState").contains(parts[4]))
      throw new IllegalStateException("Invalid call patch field");
    if (parts.length == 5) return;
    if (!"person".equals(parts[4]) || !Set.of("firstName", "lastName", "middleName",
        "age", "phone", "contactPhone", "onScenePhone", "address", "additionalInfo").contains(parts[5]))
      throw new IllegalStateException("Invalid person patch field");
  });

  private final Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation;
  private final ArrayNode previous;
  private final JsonNode step;
  private final int targetIndex;

  public DdsIntermediateStageStep(
      ObjectMapper mapper,
      int index,
      JsonNode planItems,
      ArrayNode accepted,
      JsonNode stageContext,
      LinkedHashMap<String, String> codes,
      LinkedHashMap<String, String> services,
      String ddsService,
      String userRequirements,
      String scenario,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation) {
    this(mapper, index, planItems, accepted, stageContext, codes, services, ddsService,
        userRequirements, scenario, validation, null, -1);
  }

  public DdsIntermediateStageStep(
      ObjectMapper mapper, int index, JsonNode planItems, ArrayNode accepted, JsonNode stageContext,
      LinkedHashMap<String, String> codes, LinkedHashMap<String, String> services,
      String ddsService, String userRequirements, String scenario,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation, Edit edit, int targetIndex) {
    super(
        "Промежуточный этап " + (index + 1),
        editPrompt(messages(
            mapper,
            index,
            planItems,
            accepted,
            stageContext,
            codes,
            services,
            ddsService,
            userRequirements,
            scenario), edit),
        "один stage указанного типа",
        3,
        mapper,
        edit);
    this.validation = validation;
    this.previous = accepted.deepCopy();
    this.step = planItems.get(index);
    this.targetIndex = targetIndex;
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper,
      int index,
      JsonNode planItems,
      ArrayNode accepted,
      JsonNode stageContext,
      LinkedHashMap<String, String> codes,
      LinkedHashMap<String, String> services,
      String ddsService,
      String userRequirements,
      String scenario) {
    var step = planItems.get(index);
    var stageMessages = new ArrayList<IncidentLanguageModelPort.Message>();
    stageMessages.add(
        new IncidentLanguageModelPort.Message(
            "system",
            """
Напиши ОДИН промежуточный этап ДДС. Ответ только JSON {"stage":{...}}.
stage: title (непустое), type (заданный ниже), timeLimitSeconds (целое > 0), calls (массив).
Не указывай actualStatus, expectedComment, description — здесь они не нужны.
Звонок: direction INBOUND|OUTBOUND, counterparty BRIGADE|SERVICE, person с
firstName,lastName,phone (строки), knownFacts и hiddenFacts — массивы строк.
Для SERVICE нужен serviceCode из списка и НЕ равный службе ДДС.
Для BRIGADE serviceCode НЕЛЬЗЯ указывать.
Допустимые gender: MAN|WOMEN; emotionalState: CALM|WORRIED|PANICKED|AGGRESSIVE|CONFUSED.
aiContext — краткая манера общения, ПОЛЕ ЗВОНКА, НЕ ЭТАПА и НЕ person.
actualStatus и expectedComment — поля ЭТАПА, НЕ звонка.
Пример ФОРМЫ: {"stage":{"title":"Контроль","type":"CALL_BRIGADE_FOR_STATUS",
"timeLimitSeconds":60,"calls":[{"direction":"OUTBOUND","counterparty":"BRIGADE",
"person":{"firstName":"Иван","lastName":"Петров","phone":"+79990000000"},
"knownFacts":["Бригада выехала"],"hiddenFacts":[],"gender":"MAN",
"emotionalState":"CALM","aiContext":"Говорит спокойно"}]}}.
Придумай свои факты и контакты. Не добавляй id, position и граничные этапы.
Для CALL_BRIGADE_FOR_STATUS нужен ОДИН исходящий звонок BRIGADE,
если пользователь явно не запросил больше. Никаких других звонков и служб.
Ответ компактный: не более трёх коротких известных фактов и двух скрытых.
Не повторяй уже принятые этапы; продолжай ту же ситуацию и хронологию.
"""
                + "\nКоды: "
                + mapper.writeValueAsString(codes)
                + "\nСлужбы: "
                + mapper.writeValueAsString(services)
                + "\nСлужба ДДС: "
                + ddsService
                + "\nЗапросы пользователя: "
                + userRequirements
                + "\nСуть сценария: "
                + scenario
                + "\nКарточка и основные данные: "
                + mapper.writeValueAsString(stageContext)
                + "\nПлан: "
                + mapper.writeValueAsString(planItems)
                + "\nТекущий этап ("
                + (index + 1)
                + " из "
                + planItems.size()
                + "): "
                + mapper.writeValueAsString(step)
                + "\nПредыдущие принятые этапы: "
                + mapper.writeValueAsString(stageSummaries(mapper, accepted))));
    return stageMessages;
  }

  @Override
  protected JsonNode generate(JsonNode response) {
    if (!response.isObject() || response.size() != 1 || !response.path("stage").isObject())
      throw new IllegalStateException("Expected one stage object");
    var stage = response.path("stage");
    if (!step.path("type").asText().equals(stage.path("type").asText()))
      throw new IllegalStateException("Stage type differs from plan");
    if (stage.path("calls").isArray()
        && "CALL_BRIGADE_FOR_STATUS".equals(stage.path("type").asText())
        && stage.path("calls").isEmpty())
      throw new IllegalStateException("Status call stage requires a call");
    for (var old : previous)
      if (old.equals(stage)) throw new IllegalStateException("Duplicate intermediate stage");
    var patch = mapper.createObjectNode();
    var intermediate = previous.deepCopy();
    intermediate.add(stage.deepCopy());
    patch.set("stages", intermediate);
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident().path("stages");
  }

  @Override
  protected JsonNode update(JsonNode response) {
    var stage = response.path("stage");
    if (!stage.isObject()) throw new IllegalStateException("Expected one DDS stage");
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

  static void prepareForValidation(ObjectMapper mapper, JsonNode response, JsonNode draft) {
    if (response.path("incident") instanceof ObjectNode incident) {
      incident.remove("initialAssignment");
      normalizeStages(mapper, incident, draft);
    }
  }

  static GenerateIncidentDraftUseCase.Result validateServices(
      GenerateIncidentDraftUseCase.Result result, ClassifierCatalogPort catalog) {
    for (var stage : result.incident().path("stages"))
      for (var call : stage.path("calls"))
        if ("SERVICE".equals(call.path("counterparty").asText()))
          catalog.requireService(call.path("serviceCode").asText());
    return result;
  }

  private static void normalizeStages(ObjectMapper mapper, ObjectNode incident, JsonNode draft) {
    var generated = incident.path("stages");
    if (!generated.isArray() || generated.isEmpty()) return;
    var stages = mapper.createArrayNode();
    var draftStages = draft.path("stages");
    var first = draftStages.get(0);
    if (first != null && first.isObject() && "ASSIGN_BRIGADE".equals(first.path("type").asText())) {
      stages.add(first.deepCopy());
    } else {
      stages.add(mapper.createObjectNode().put("title", "Получение карточки")
          .put("type", "ASSIGN_BRIGADE").put("timeLimitSeconds", 30)
          .set("calls", mapper.createArrayNode()));
    }
    for (var stage : generated) {
      if (stage.isObject() && ("ASSIGN_BRIGADE".equals(stage.path("type").asText())
          || "COMPLETE_INCIDENT".equals(stage.path("type").asText()))) continue;
      stages.add(stage.deepCopy());
    }
    var last = draftStages.get(draftStages.size() - 1);
    if (stages.size() == 1 && (last == null || !"COMPLETE_INCIDENT".equals(last.path("type").asText())))
      throw new IncidentGenerationException("Не удалось получить корректный ответ модели",
          new IllegalStateException("DDS stages require an intermediate stage"));
    if (last != null && last.isObject() && "COMPLETE_INCIDENT".equals(last.path("type").asText())
        && last != first) {
      stages.add(last.deepCopy());
    } else {
      stages.add(mapper.createObjectNode().put("title", "Завершение реагирования")
          .put("type", "COMPLETE_INCIDENT").put("timeLimitSeconds", 60)
          .set("calls", mapper.createArrayNode()));
    }
    incident.set("stages", stages);

    var knownStages = new java.util.HashSet<String>();
    var knownCalls = new java.util.HashSet<String>();
    for (var stage : draftStages) {
      if (stage.path("id").isTextual()) knownStages.add(stage.path("id").asText());
      for (var call : stage.path("calls"))
        if (call.path("id").isTextual()) knownCalls.add(call.path("id").asText());
    }
    var originalFirst = draftStages.get(0);
    for (int index = 0; index < stages.size(); index++) {
      var stage = stages.get(index);
      if (!(stage instanceof ObjectNode object)) continue;
      object.remove("position");
      var id = stage.path("id");
      if (index == 0 && originalFirst != null && originalFirst.path("id").isTextual()
          && "ASSIGN_BRIGADE".equals(stage.path("type").asText())
          && !originalFirst.path("id").asText().equals(id.asText())) {
        object.put("id", originalFirst.path("id").asText());
      } else if (id.isTextual() && !knownStages.contains(id.asText())) {
        object.remove("id");
      }
      for (var call : stage.path("calls")) {
        if (!(call instanceof ObjectNode callObject)) continue;
        callObject.remove("position");
        var callId = call.path("id");
        if (callId.isTextual() && !knownCalls.contains(callId.asText())) callObject.remove("id");
        if (call.path("person") instanceof ObjectNode person) {
          for (var field : List.of("gender", "emotionalState", "aiContext")) {
            if (!callObject.hasNonNull(field) && person.hasNonNull(field))
              callObject.set(field, person.path(field).deepCopy());
            person.remove(field);
          }
        }
      }
    }
  }

  private static ArrayNode stageSummaries(ObjectMapper mapper, ArrayNode accepted) {
    var result = mapper.createArrayNode();
    for (var stage : accepted) {
      var summary =
          mapper
              .createObjectNode()
              .put("type", stage.path("type").asText())
              .put("title", stage.path("title").asText());
      var facts = mapper.createArrayNode();
      for (var call : stage.path("calls"))
        for (var fact : call.path("knownFacts")) if (facts.size() < 12) facts.add(fact.deepCopy());
      summary.set("knownFacts", facts);
      result.add(summary);
    }
    return result;
  }
}
