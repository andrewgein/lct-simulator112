package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.ClassifierUnavailableException;
import com.simulator112.incident.application.service.IncidentGenerationLimitException;
import com.simulator112.incident.application.service.OllamaUnavailableException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentGenerationControllerTest {
    @Test
    void mapsGeneratorFailuresToBadGateway() {
        var response = new IncidentExceptionHandler().generationFailed(new IncidentGenerationException("provider failed"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).isEqualTo("Не удалось получить корректный ответ модели");
    }

    @Test
    void reportsClassifierFailureSeparately() {
        var response = new IncidentExceptionHandler().generationFailed(new ClassifierUnavailableException(null));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).isEqualTo("Не удалось получить коды классификатора. Попробуйте позже");
    }

    @Test
    void reportsOllamaConnectionFailureSeparately() {
        var response = new IncidentExceptionHandler().generationFailed(new OllamaUnavailableException(
                "Нет соединения с Ollama. Проверьте доступность сервера модели", new java.net.ConnectException()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).contains("Нет соединения с Ollama");
    }

    @Test
    void reportsGenerationLimit() {
        var response = new IncidentExceptionHandler().generationFailed(new IncidentGenerationLimitException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).contains("меньше этапов или звонков");
    }

    @Test
    void streamsStatusAndResult() throws Exception {
        var mapper = new ObjectMapper();
        GenerateIncidentDraftUseCase useCase = command -> {
            command.onStatus().accept("Генерирую план");
            return new GenerateIncidentDraftUseCase.Result("Готово", mapper.readTree("{\"title\":\"Пожар\"}"));
        };
        var controller = new IncidentGenerationController(useCase, mapper);
        var response = controller.stream(new IncidentGenerationController.GenerateRequest(
                List.of(new IncidentGenerationController.Message("user", "Пожар")), mapper.readTree("{}")));
        var output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);
        var events = output.toString(StandardCharsets.UTF_8);
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.TEXT_EVENT_STREAM);
        assertThat(events).contains("event: status\ndata: \"Генерирую план\"\n\n");
        assertThat(events).contains("event: result\ndata: {\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\"}}\n\n");
    }

    @Test
    void streamsFailureAsErrorEvent() throws Exception {
        var mapper = new ObjectMapper();
        GenerateIncidentDraftUseCase useCase = command -> {
            command.onStatus().accept("Генерирую план");
            throw new OllamaUnavailableException("Модель недоступна", new java.net.ConnectException());
        };
        var output = new ByteArrayOutputStream();
        new IncidentGenerationController(useCase, mapper).stream(new IncidentGenerationController.GenerateRequest(
                List.of(new IncidentGenerationController.Message("user", "Пожар")), mapper.readTree("{}")))
                .getBody().writeTo(output);
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("event: error\ndata: \"Модель недоступна\"\n\n");
    }

    @Test
    void delegatesToUseCaseAndPreservesHttpResponseShape() {
        var mapper = new ObjectMapper();
        var draft = mapper.readTree("{\"title\":\"Пожар\"}");
        var patch = mapper.readTree("{\"difficulty\":\"HARD\"}");
        var received = new AtomicReference<GenerateIncidentDraftUseCase.Command>();
        GenerateIncidentDraftUseCase useCase = command -> {
            received.set(command);
            return new GenerateIncidentDraftUseCase.Result("Готово", patch);
        };
        var response = new IncidentGenerationController(useCase, mapper).generate(new IncidentGenerationController.GenerateRequest(
                List.of(new IncidentGenerationController.Message("user", "Сделай сложнее")), draft));
        assertThat(received.get().draft()).isSameAs(draft);
        assertThat(received.get().messages()).containsExactly(new GenerateIncidentDraftUseCase.Message("user", "Сделай сложнее"));
        assertThat(response.message()).isEqualTo("Готово");
        assertThat(response.incident()).isSameAs(patch);
    }
}
