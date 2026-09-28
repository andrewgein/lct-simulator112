package com.simulator112.incident.adapter.out.ollama;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.IncidentGenerationLimitException;
import com.simulator112.incident.application.service.OllamaUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaIncidentGenerationAdapter implements IncidentLanguageModelPort {
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final URI uri;
    private final String model;

    public OllamaIncidentGenerationAdapter(ObjectMapper mapper,
            @Value("${incident.generator.url:http://100.85.141.32:11434/api/chat}") String url,
            @Value("${incident.generator.model:qwen3:4b-instruct-2507-q4_K_M}") String model) {
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.uri = URI.create(url);
        this.model = model;
    }

    @Override
    public String generate(List<Message> messages) {
        try {
            var body = mapper.writeValueAsString(Map.of("model", model, "stream", false, "format", "json", "keep_alive", "10m",
                    "messages", messages, "options", Map.of("temperature", 0.3, "num_ctx", 16384, "num_predict", 8192)));
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IncidentGenerationException("Ollama HTTP " + response.statusCode());
            }
            var result = mapper.readTree(response.body());
            if ("length".equals(result.path("done_reason").asText())) {
                throw new IncidentGenerationLimitException();
            }
            var content = result.path("message").path("content");
            if (!content.isTextual()) throw new IncidentGenerationException("Ollama returned no message content");
            return content.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IncidentGenerationException("Генерация прервана", e);
        } catch (HttpConnectTimeoutException e) {
            throw new OllamaUnavailableException("Нет соединения с Ollama. Проверьте доступность сервера модели", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaUnavailableException("Ollama не ответила вовремя", e);
        } catch (IncidentGenerationException e) {
            throw e;
        } catch (IOException e) {
            throw new OllamaUnavailableException("Ошибка связи с Ollama. Проверьте доступность сервера модели", e);
        } catch (Exception e) {
            throw new IncidentGenerationException("Не удалось получить ответ модели", e);
        }
    }
}
