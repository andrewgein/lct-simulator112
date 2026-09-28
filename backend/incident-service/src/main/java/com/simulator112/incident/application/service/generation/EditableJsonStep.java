package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public abstract class EditableJsonStep extends JsonResponseStep<JsonNode> {
    public record Edit(JsonNode draft, List<String> paths) {
        public Edit {
            paths = List.copyOf(paths);
        }
    }

    protected final Edit edit;

    protected EditableJsonStep(String name, List<IncidentLanguageModelPort.Message> prompt,
            String expectedResult, int maxAttempts, ObjectMapper mapper, Edit edit) {
        super(name, prompt, expectedResult, maxAttempts, mapper);
        this.edit = edit;
    }

    protected static List<IncidentLanguageModelPort.Message> editPrompt(
            List<IncidentLanguageModelPort.Message> messages, Edit edit) {
        if (edit == null) return messages;
        var result = new ArrayList<>(messages);
        var first = result.getFirst();
        result.set(0, new IncidentLanguageModelPort.Message("system", first.content() + """

                РЕЖИМ РЕДАКТИРОВАНИЯ существующего сценария. Изменяй только выбранные поля:
                """ + String.join(", ", edit.paths()) + """
                . Остальные поля и технические ID сервер сохранит из черновика.
                Не создавай другой сценарий; согласуй новые значения с существующими данными.
                Ответ в прежней JSON-форме; достаточно указать выбранные поля.
                """));
        return result;
    }

    @Override
    protected final JsonNode validateJson(JsonNode response) {
        return edit == null ? generate(response) : update(response);
    }

    protected abstract JsonNode generate(JsonNode response);

    protected abstract JsonNode update(JsonNode response);
}
