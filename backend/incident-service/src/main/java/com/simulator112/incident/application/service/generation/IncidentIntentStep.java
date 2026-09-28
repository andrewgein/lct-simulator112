package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class IncidentIntentStep extends JsonResponseStep<IncidentIntentStep.Decision> {
    public enum Decision { ANSWER, CREATE, UPDATE }

    @Override protected String status() { return "Определяю тип запроса"; }

    public IncidentIntentStep(ObjectMapper mapper, JsonNode draft, List<GenerateIncidentDraftUseCase.Message> history) {
        super("Определение намерения", messages(draft, history),
                "JSON с intent (ANSWER, CREATE или UPDATE)", 2, mapper);
    }

    private static List<IncidentLanguageModelPort.Message> messages(JsonNode draft,
            List<GenerateIncidentDraftUseCase.Message> history) {
        var messages = new ArrayList<IncidentLanguageModelPort.Message>();
        messages.add(new IncidentLanguageModelPort.Message("system", """
                Определи намерение ПОСЛЕДНЕГО сообщения пользователя в чате конструктора учебных сценариев.
                Классифицируй по ожидаемому результату, а не по отдельным словам или форме фразы:
                - CREATE: пользователь хочет получить целый новый сценарий или полностью пересоздать его.
                - UPDATE: пользователь хочет изменить только конкретную часть черновика, даже если черновик пока пуст.
                - ANSWER: пользователь хочет получить текстовое объяснение без изменения сценария,
                  в том числе если тема вопроса не связана с симулятором.
                Приветствие и вопросительная форма не меняют намерение. Не проверяй, есть ли нужные поля
                в черновике: создание отдельного поля — UPDATE, а не генерация всего сценария.
                Не отвечай пользователю и не меняй сценарий на этом шаге.
                Ответ только JSON с одним полем intent: CREATE, UPDATE или ANSWER.
                """ + "\nТип сценария: " + draft.path("targetType").asText("SYSTEM_112")));
        history.forEach(item -> messages.add(new IncidentLanguageModelPort.Message(item.role(), item.content())));
        return messages;
    }

    @Override
    protected Decision validateJson(JsonNode response) {
        if (!response.isObject() || response.size() != 1) throw new IllegalStateException("Invalid intent response");
        var intent = response.path("intent").asText();
        return switch (intent) {
            case "CREATE" -> Decision.CREATE;
            case "UPDATE" -> Decision.UPDATE;
            case "ANSWER" -> Decision.ANSWER;
            default -> throw new IllegalStateException("Invalid intent response");
        };
    }
}
