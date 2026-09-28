package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody GenerateRequest request) {
        StreamingResponseBody body = output -> {
            try {
                var result = generateDraft.generate(command(request, status -> {
                    try { send(output, "status", status); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                }));
                send(output, "result", new GenerateResponse(result.message(), result.incident()));
            } catch (UncheckedIOException e) {
                // Client disconnected; do not send another event to the broken stream.
            } catch (Exception e) {
                var message = e instanceof com.simulator112.incident.application.service.ClassifierUnavailableException
                        || e instanceof com.simulator112.incident.application.service.IncidentGenerationLimitException
                        || e instanceof com.simulator112.incident.application.service.OllamaUnavailableException
                        || e instanceof IllegalArgumentException ? e.getMessage() : "Не удалось получить корректный ответ модели";
                send(output, "error", message);
            }
        };
        return ResponseEntity.ok().header("Cache-Control", "no-cache").header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM).body(body);
    }

    private void send(java.io.OutputStream output, String event, Object data) throws IOException {
        output.write(("event: " + event + "\ndata: " + mapper.writeValueAsString(data) + "\n\n")
                .getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
