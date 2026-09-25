package com.simulator112.incident.application.port.in;

import tools.jackson.databind.JsonNode;
import java.util.List;

public interface GenerateIncidentDraftUseCase {
    Result generate(Command command);

    record Message(String role, String content) {}
    record Command(List<Message> messages, JsonNode draft) {}
    record Result(String message, JsonNode incident) {}
}
