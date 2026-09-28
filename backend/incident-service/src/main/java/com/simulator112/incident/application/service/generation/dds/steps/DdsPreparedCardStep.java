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
import tools.jackson.databind.node.ObjectNode;

public final class DdsPreparedCardStep extends EditableJsonStep {
  @Override protected String status() { return "Заполняю заявителя и карточку"; }
  public static final StepPaths PATHS = new StepPaths(Set.of("preparedCardTemplate"),
      "Карточка ДДС: /preparedCardTemplate, /preparedCardTemplate/applicant/phone и другие поля карточки.",
      (draft, parts) -> {
    if (parts.length > 3 || parts.length >= 2
        && !Set.of("classifierCodes", "victimCount", "applicant", "additionalInfo").contains(parts[1])
        || parts.length == 3 && !("applicant".equals(parts[1])
            && Set.of("firstName", "lastName", "middleName", "age", "phone", "contactPhone",
                "onScenePhone", "address", "additionalInfo").contains(parts[2])
            || "additionalInfo".equals(parts[1]) && !parts[2].isBlank()))
      throw new IllegalStateException("Invalid card patch path");
  });

  private final Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation;

  public DdsPreparedCardStep(
      ObjectMapper mapper,
      String scenario,
      JsonNode incident,
      String userRequirements,
      LinkedHashMap<String, String> codes,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation) {
    this(mapper, scenario, incident, userRequirements, codes, validation, null);
  }

  public DdsPreparedCardStep(
      ObjectMapper mapper, String scenario, JsonNode incident, String userRequirements,
      LinkedHashMap<String, String> codes,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation, Edit edit) {
    super("Подготовленная карточка", editPrompt(messages(mapper, scenario, incident, userRequirements, codes), edit),
        "preparedCardTemplate с кодами, victimCount и заявителем", 3, mapper, edit);
    this.validation = validation;
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper,
      String scenario,
      JsonNode incident,
      String userRequirements,
      LinkedHashMap<String, String> codes) {
    var cardMessages = new ArrayList<IncidentLanguageModelPort.Message>();
    cardMessages.add(
        new IncidentLanguageModelPort.Message(
            "system",
            """
            Из ситуации создай ТОЛЬКО подготовленную карточку ДДС. Ответ только JSON:
            {"message":"Готово","incident":{"preparedCardTemplate":{"classifierCodes":["код"],
            "victimCount":0,"applicant":{"firstName":"Иван","lastName":"Петров",
            "phone":"+79990000000"}}}}.
            Обязательно придумай отдельного заявителя с НЕПУСТЫМИ firstName, lastName и phone.
            Примерные имя и телефон замени. Можно добавить middleName, age (целое >=0),
            contactPhone, onScenePhone, address, additionalInfo. Адрес заявителя — одна
            строка, не объект с городом и улицей. Он сообщил о происшествии
            до передачи карточки в ДДС. Не путай его с сотрудником бригады в звонках.
            Выбери подходящие коды только из списка; victimCount — число пострадавших >=0.
            Не добавляй assignedServices, initialAssignment, stages и другие поля incident.
            """
                + "\nСитуация: "
                + scenario
                + "\nОсновные данные: "
                + mapper.writeValueAsString(incident)
                + "\nЗапросы пользователя: "
                + userRequirements
                + "\nКоды классификатора: "
                + mapper.writeValueAsString(codes)));
    return cardMessages;
  }

  @Override
  protected JsonNode generate(JsonNode response) {
    normalizeApplicantAddress(response);
    var fields = response.path("incident");
    var prepared = fields.path("preparedCardTemplate");
    if (!fields.isObject()
        || fields.size() != 1
        || !prepared.path("classifierCodes").isArray()
        || prepared.path("classifierCodes").isEmpty()
        || !prepared.path("victimCount").isIntegralNumber()
        || !hasRequiredApplicant(prepared.path("applicant")))
      throw new IllegalStateException("Incomplete DDS prepared card or applicant");
    return validation.apply(response).incident().path("preparedCardTemplate").deepCopy();
  }

  @Override
  protected JsonNode update(JsonNode response) {
    normalizeApplicantAddress(response);
    var patch = SelectedFields.apply(mapper, edit.draft(), response.path("incident"), edit.paths());
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident();
  }

  private static void normalizeApplicantAddress(JsonNode response) {
    var applicant = response.path("incident").path("preparedCardTemplate").path("applicant");
    if (!(applicant instanceof ObjectNode person) || !person.path("address").isObject()) return;
    var address = person.path("address");
    var parts = List.of("city", "street", "house", "building", "apartment", "floor");
    if (address.properties().stream().anyMatch(entry -> !parts.contains(entry.getKey())
        || !entry.getValue().isNull() && !entry.getValue().isTextual() && !entry.getValue().isNumber())) return;
    var text = new ArrayList<String>();
    for (var part : parts) {
      var value = address.path(part);
      if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank())
        text.add(switch (part) {
          case "house" -> "д. " + value.asText();
          case "building" -> "корп. " + value.asText();
          case "apartment" -> "кв. " + value.asText();
          case "floor" -> "этаж " + value.asText();
          default -> value.asText();
        });
    }
    if (!text.isEmpty()) person.put("address", String.join(", ", text));
  }

  static void prepareForValidation(JsonNode response) {
    if (response.path("incident").path("preparedCardTemplate") instanceof ObjectNode card) {
      card.remove("assignedServices");
      if (card.isEmpty() && response.path("incident") instanceof ObjectNode incident)
        incident.remove("preparedCardTemplate");
    }
  }

  static GenerateIncidentDraftUseCase.Result validateServices(
      GenerateIncidentDraftUseCase.Result result, ClassifierCatalogPort catalog, ObjectMapper mapper) {
    var codes = result.incident().path("preparedCardTemplate").path("classifierCodes");
    if (codes.isArray()) {
      var selected = new ArrayList<String>();
      codes.forEach(code -> selected.add(code.asText()));
      var assigned = catalog.resolveAssignedServices(selected);
      if (assigned.size() > 100 || assigned.stream().anyMatch(code -> code == null || code.isBlank())
          || assigned.stream().distinct().count() != assigned.size())
        throw new IncidentGenerationException("Некорректные службы из классификатора");
      var card = (ObjectNode) result.incident().path("preparedCardTemplate");
      card.set("assignedServices", mapper.valueToTree(assigned));
    }
    for (var service : result.incident().path("preparedCardTemplate").path("assignedServices"))
      catalog.requireService(service.asText());
    return result;
  }

  private static boolean hasRequiredApplicant(JsonNode applicant) {
    if (!applicant.isObject()) return false;
    for (var field : List.of("firstName", "lastName", "phone")) {
      if (!applicant.path(field).isTextual() || applicant.path(field).asText().isBlank())
        return false;
    }
    return true;
  }
}
