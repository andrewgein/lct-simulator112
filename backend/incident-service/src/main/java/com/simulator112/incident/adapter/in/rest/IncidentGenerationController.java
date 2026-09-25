package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents/generate")
@RequiredArgsConstructor
public class IncidentGenerationController {
    private final GenerateIncidentDraftUseCase generateDraft;

    public record Message(String role, String content) {}
    public record GenerateRequest(List<Message> messages, JsonNode draft) {}
    public record GenerateResponse(String message, JsonNode incident) {}

    @PostMapping
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        var messages = request.messages() == null ? null : request.messages().stream()
                .map(message -> message == null ? null : new GenerateIncidentDraftUseCase.Message(message.role(), message.content()))
                .toList();
        var result = generateDraft.generate(new GenerateIncidentDraftUseCase.Command(messages, request.draft()));
        return new GenerateResponse(result.message(), result.incident());
    }
}
