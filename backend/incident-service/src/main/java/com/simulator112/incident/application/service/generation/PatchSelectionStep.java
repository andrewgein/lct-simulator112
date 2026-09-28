package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PatchSelectionStep extends JsonResponseStep<List<String>> {
    @Override protected String status() { return "Определяю изменения"; }
    private final JsonNode draft;
    private final List<StepPaths> steps;

    public PatchSelectionStep(ObjectMapper mapper, JsonNode draft, List<GenerateIncidentDraftUseCase.Message> history,
            boolean dds, List<StepPaths> steps) {
        super("Выбор полей для правки", prompt(mapper, draft, history, dds, steps),
                "JSON с paths — массивом путей к изменяемым полям", 3, mapper);
        this.draft = draft;
        this.steps = List.copyOf(steps);
    }

    private static List<IncidentLanguageModelPort.Message> prompt(ObjectMapper mapper, JsonNode draft,
            List<GenerateIncidentDraftUseCase.Message> history, boolean dds, List<StepPaths> steps) {
        var messages = new ArrayList<IncidentLanguageModelPort.Message>();
        messages.add(new IncidentLanguageModelPort.Message("system", """
                Выбери ТОЛЬКО поля, которые пользователь попросил изменить в существующем сценарии.
                Ответ только JSON: {"paths":["/difficulty","/address/city"]}.
                paths — от 1 до 20 JSON-путей к существующим полям; НЕ создавай текст сценария.
                Укажи только явно затронутые поля, не добавляй соседние поля.
                Допустимые пути перечислены шагами ниже. Не выбирай id, position,
                initialAssignment и assignedServices. Индексы массивов начинаются с 0.
                Выбирай /stages целиком только при прямом
                запросе создать, удалить или перестроить этапы, а не при правке одного элемента.
                """ + "\nТип сценария: " + (dds ? "DDS" : "SYSTEM_112")
                + "\nДоступные шаги:\n" + String.join("\n", steps.stream().map(StepPaths::guidance).toList())
                + "\nЧерновик: " + mapper.writeValueAsString(draft)));
        history.forEach(message -> messages.add(new IncidentLanguageModelPort.Message(message.role(), message.content())));
        return messages;
    }

    @Override
    protected List<String> validateJson(JsonNode response) {
        var values = response.path("paths");
        if (!response.isObject() || response.size() != 1 || !values.isArray() || values.isEmpty() || values.size() > 20)
            throw new IllegalStateException("Invalid patch selection");
        var paths = new LinkedHashSet<String>();
        for (var value : values) {
            if (!value.isTextual()) throw new IllegalStateException("Invalid patch path");
            var path = value.asText();
            if (!path.startsWith("/") || path.contains("~") || path.endsWith("/"))
                throw new IllegalStateException("Invalid patch path");
            var step = steps.stream().filter(candidate -> candidate.matches(path)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unknown patch field"));
            step.check(path, draft);
            if (!paths.add(path)) throw new IllegalStateException("Duplicate patch path");
        }
        for (var first : paths) for (var second : paths) if (!first.equals(second) && second.startsWith(first + "/"))
            throw new IllegalStateException("Overlapping patch paths");
        return List.copyOf(paths);
    }
}
