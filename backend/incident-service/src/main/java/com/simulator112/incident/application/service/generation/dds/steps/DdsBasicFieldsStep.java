package com.simulator112.incident.application.service.generation.dds.steps;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.generation.EditableJsonStep;
import com.simulator112.incident.application.service.generation.SelectedFields;
import com.simulator112.incident.application.service.generation.StepPaths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class DdsBasicFieldsStep extends EditableJsonStep {
  @Override protected String status() { return "Заполняю адрес и основные данные"; }
  public static final StepPaths PATHS = new StepPaths(Set.of("title", "difficulty", "address"),
      "Основные данные: /title, /difficulty, /address или /address/city (и другие поля адреса).",
      (draft, parts) -> {
        if (List.of("title", "difficulty").contains(parts[0]) && parts.length != 1)
          throw new IllegalStateException("Invalid scalar patch path");
        if ("address".equals(parts[0]) && (parts.length > 2 || parts.length == 2
            && !Set.of("city", "street", "house", "building", "apartment", "floor").contains(parts[1])))
          throw new IllegalStateException("Invalid address patch path");
      });

  private final Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation;

  public DdsBasicFieldsStep(
      ObjectMapper mapper,
      String scenario,
      String userRequirements,
      JsonNode modelDraft,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation) {
    this(mapper, scenario, userRequirements, modelDraft, validation, null);
  }

  public DdsBasicFieldsStep(
      ObjectMapper mapper, String scenario, String userRequirements, JsonNode modelDraft,
      Function<JsonNode, GenerateIncidentDraftUseCase.Result> validation, Edit edit) {
    super("Основные данные", editPrompt(messages(mapper, scenario, userRequirements, modelDraft), edit),
        "incident с title и difficulty", 3, mapper, edit);
    this.validation = validation;
  }

  private static List<IncidentLanguageModelPort.Message> messages(
      ObjectMapper mapper, String scenario, String userRequirements, JsonNode modelDraft) {
    var metadataMessages = new ArrayList<IncidentLanguageModelPort.Message>();
    metadataMessages.add(
        new IncidentLanguageModelPort.Message(
            "system",
            """
            Из описания создай ОСНОВНЫЕ ДАННЫЕ ДДС. Только JSON:
            {"message":"Готово","incident":{"title":"...","difficulty":"NORMAL",
            "address":{"city":"...","street":"...","house":"..."}}}.
            title непустой, difficulty EASY|NORMAL|HARD. Адрес добавляй только если он известен
            или его можно правдоподобно дополнить, не меняя данных пользователя.
            Не добавляй подготовленную карточку, заявителя, план, этапы и звонки.
            """
                + "\nСитуация: "
                + scenario
                + "\nЗапросы пользователя: "
                + userRequirements
                + "\nЧерновик: "
                + mapper.writeValueAsString(modelDraft)));
    return metadataMessages;
  }

  @Override
  protected JsonNode generate(JsonNode response) {
    var fields = response.path("incident");
    if (!fields.isObject()
        || !fields.path("title").isTextual()
        || fields.path("title").asText().isBlank()
        || !fields.path("difficulty").isTextual()
        || fields.properties().stream()
            .anyMatch(entry -> !List.of("title", "difficulty", "address").contains(entry.getKey())))
      throw new IllegalStateException("Incomplete DDS basic fields");
    return validation.apply(response).incident().deepCopy();
  }

  @Override
  protected JsonNode update(JsonNode response) {
    var patch = SelectedFields.apply(mapper, edit.draft(), response.path("incident"), edit.paths());
    var envelope = mapper.createObjectNode().put("message", "Готово");
    envelope.set("incident", patch);
    return validation.apply(envelope).incident();
  }
}
