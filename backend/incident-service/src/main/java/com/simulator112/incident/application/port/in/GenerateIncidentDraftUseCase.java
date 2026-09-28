package com.simulator112.incident.application.port.in;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Consumer;

public interface GenerateIncidentDraftUseCase {
    Result generate(Command command);

    record Message(String role, String content) {}
    record Command(List<Message> messages, JsonNode draft, Consumer<String> onStatus) {
        public Command(List<Message> messages, JsonNode draft) {
            this(messages, draft, status -> {});
        }
    }
    record Result(String message, JsonNode incident) {}
}
