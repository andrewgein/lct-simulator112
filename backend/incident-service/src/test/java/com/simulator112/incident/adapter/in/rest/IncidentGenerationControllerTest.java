package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.application.port.in.GenerateIncidentDraftUseCase;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.ClassifierUnavailableException;
import com.simulator112.incident.application.service.IncidentGenerationLimitException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
    void reportsGenerationLimit() {
        var response = new IncidentExceptionHandler().generationFailed(new IncidentGenerationLimitException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).contains("меньше этапов или звонков");
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
        var response = new IncidentGenerationController(useCase).generate(new IncidentGenerationController.GenerateRequest(
                List.of(new IncidentGenerationController.Message("user", "Сделай сложнее")), draft));
        assertThat(received.get().draft()).isSameAs(draft);
        assertThat(received.get().messages()).containsExactly(new GenerateIncidentDraftUseCase.Message("user", "Сделай сложнее"));
        assertThat(response.message()).isEqualTo("Готово");
        assertThat(response.incident()).isSameAs(patch);
    }
}
