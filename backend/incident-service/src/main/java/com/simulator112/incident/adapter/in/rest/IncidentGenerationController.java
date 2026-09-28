package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final ObjectMapper mapper;

    public record Message(String role, String content) {}
    public record GenerateRequest(List<Message> messages, JsonNode draft) {}
    public record GenerateResponse(String message, JsonNode incident) {}

    private GenerateIncidentDraftUseCase.Command command(GenerateRequest request, java.util.function.Consumer<String> onStatus) {
        var messages = request.messages() == null ? null : request.messages().stream()
                .map(message -> message == null ? null : new GenerateIncidentDraftUseCase.Message(message.role(), message.content()))
                .toList();
        return new GenerateIncidentDraftUseCase.Command(messages, request.draft(), onStatus);
    }

    @PostMapping
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        var result = generateDraft.generate(command(request, status -> {}));
        return new GenerateResponse(result.message(), result.incident());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestBody GenerateRequest request) {
        var emitter = new SseEmitter(15 * 60 * 1000L);
        Thread.ofVirtual().start(() -> {
            try {
                var result = generateDraft.generate(command(request, status -> send(emitter, "status", status)));
                send(emitter, "result", new GenerateResponse(result.message(), result.incident()));
                emitter.complete();
            } catch (Exception e) {
                var message = e instanceof com.simulator112.incident.application.service.ClassifierUnavailableException
                        || e instanceof com.simulator112.incident.application.service.IncidentGenerationLimitException
                        || e instanceof com.simulator112.incident.application.service.OllamaUnavailableException
                        || e instanceof IllegalArgumentException ? e.getMessage() : "Не удалось получить корректный ответ модели";
                try {
                    send(emitter, "error", message);
                    emitter.complete();
                } catch (RuntimeException disconnected) {
                    emitter.completeWithError(disconnected);
                }
            }
        });
        return ResponseEntity.ok().header("Cache-Control", "no-cache").header("X-Accel-Buffering", "no")
                .contentType(new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8)).body(emitter);
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(mapper.writeValueAsString(data), new MediaType("text", "plain", StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
