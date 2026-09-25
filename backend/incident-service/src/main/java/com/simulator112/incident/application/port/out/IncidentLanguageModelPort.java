package com.simulator112.incident.application.port.out;

import java.util.List;

public interface IncidentLanguageModelPort {
    String generate(List<Message> messages);

    record Message(String role, String content) {}
}
