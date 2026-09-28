package com.simulator112.incident.adapter.out.ollama;

import com.sun.net.httpserver.HttpServer;
import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.IncidentGenerationLimitException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaIncidentGenerationAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsJsonFormatAndReturnsModelContent() throws Exception {
        var requestBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
            var body = mapper.writeValueAsBytes(Map.of("message", Map.of("content", "{\"incident\":{}}")));
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var adapter = new OllamaIncidentGenerationAdapter(mapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat", "test-model");
            assertThat(adapter.generate(List.of(new IncidentLanguageModelPort.Message("user", "Пожар"))))
                    .isEqualTo("{\"incident\":{}}");
            var sent = mapper.readTree(requestBody.get());
            assertThat(sent.path("model").asText()).isEqualTo("test-model");
            assertThat(sent.path("format").asText()).isEqualTo("json");
            assertThat(sent.path("options").path("num_ctx").asInt()).isEqualTo(16384);
            assertThat(sent.path("options").path("num_predict").asInt()).isEqualTo(8192);
            assertThat(sent.path("keep_alive").asText()).isEqualTo("10m");
            assertThat(sent.path("messages").get(0).path("role").asText()).isEqualTo("user");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsTruncatedResponse() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            var body = mapper.writeValueAsBytes(Map.of("done_reason", "length",
                    "message", Map.of("content", "{\"incident\":")));
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var adapter = new OllamaIncidentGenerationAdapter(mapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat", "test-model");
            assertThatThrownBy(() -> adapter.generate(List.of(new IncidentLanguageModelPort.Message("user", "Пожар"))))
                    .isInstanceOf(IncidentGenerationLimitException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingContent() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            var body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var adapter = new OllamaIncidentGenerationAdapter(mapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat", "test-model");
            assertThatThrownBy(() -> adapter.generate(List.of(new IncidentLanguageModelPort.Message("user", "Пожар"))))
                    .isInstanceOf(IncidentGenerationException.class);
        } finally {
            server.stop(0);
        }
    }
}
