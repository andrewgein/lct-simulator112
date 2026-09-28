package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.ClassifierUnavailableException;
import com.simulator112.incident.application.service.IncidentGenerationLimitException;
import com.simulator112.incident.application.service.OllamaUnavailableException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    void streamsStatusBeforeGenerationCompletes() throws Exception {
        var mapper = new ObjectMapper();
        var statusSent = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        GenerateIncidentDraftUseCase useCase = command -> {
            command.onStatus().accept("Генерирую план");
            statusSent.countDown();
            try {
                if (!resume.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Generation did not resume");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return new GenerateIncidentDraftUseCase.Result("Готово", mapper.readTree("{\"title\":\"Пожар\"}"));
        };
        var mvc = MockMvcBuilders.standaloneSetup(new IncidentGenerationController(useCase, mapper)).build();
        var request = mvc.perform(post("/api/v1/incidents/generate/stream")
                .contentType("application/json").content("{\"messages\":[{\"role\":\"user\",\"content\":\"Пожар\"}],\"draft\":{}}"))
                .andReturn();
        try {
            assertThat(statusSent.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(request.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("event:status\ndata:\"Генерирую план\"\n\n")
                    .doesNotContain("event: result");
        } finally {
            resume.countDown();
        }
        var response = mvc.perform(asyncDispatch(request)).andReturn().getResponse();
        assertThat(response.getContentType()).startsWith("text/event-stream");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("event:result\ndata:{\"message\":\"Готово\",\"incident\":{\"title\":\"Пожар\"}}\n\n");
    }

    @Test
    void streamsFailureAsErrorEvent() throws Exception {
        var mapper = new ObjectMapper();
        GenerateIncidentDraftUseCase useCase = command -> {
            command.onStatus().accept("Генерирую план");
            throw new OllamaUnavailableException("Модель недоступна", new java.net.ConnectException());
        };
        var mvc = MockMvcBuilders.standaloneSetup(new IncidentGenerationController(useCase, mapper)).build();
        var request = mvc.perform(post("/api/v1/incidents/generate/stream")
                .contentType("application/json").content("{\"messages\":[{\"role\":\"user\",\"content\":\"Пожар\"}],\"draft\":{}}"))
                .andReturn();
        var response = mvc.perform(asyncDispatch(request)).andReturn().getResponse();
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("event:error\ndata:\"Модель недоступна\"\n\n");
    }

    @Test
    void streamsLinkageFailureWithoutExposingServerInternals() throws Exception {
        var mapper = new ObjectMapper();
        GenerateIncidentDraftUseCase useCase = command -> {
            command.onStatus().accept("Генерирую карточку");
            throw new NoClassDefFoundError("com/simulator112/classifier/grpc/contract/ClassifierServiceOuterClass");
        };
        var mvc = MockMvcBuilders.standaloneSetup(new IncidentGenerationController(useCase, mapper)).build();
        var request = mvc.perform(post("/api/v1/incidents/generate/stream")
                .contentType("application/json").content("{\"messages\":[{\"role\":\"user\",\"content\":\"Пожар\"}],\"draft\":{}}"))
                .andReturn();
        var response = mvc.perform(asyncDispatch(request)).andReturn().getResponse();
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("event:status", "event:error\ndata:\"Внутренняя ошибка сервера при генерации сценария\"\n\n")
                .doesNotContain("ClassifierServiceOuterClass");
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
