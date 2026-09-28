package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import com.simulator112.incident.application.service.IncidentGenerationException;
import com.simulator112.incident.application.service.OllamaUnavailableException;

import java.util.ArrayList;
import java.util.List;

public abstract class GenerationStep<T> {
    private final String name;
    private final List<IncidentLanguageModelPort.Message> prompt;
    private final String expectedResult;
    private final int maxAttempts;

    protected GenerationStep(String name, List<IncidentLanguageModelPort.Message> prompt,
            String expectedResult, int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        this.name = name;
        this.prompt = List.copyOf(prompt);
        this.expectedResult = expectedResult;
        this.maxAttempts = maxAttempts;
    }

    protected abstract T validate(String modelResponse);

    private T execute(IncidentLanguageModelPort model) {
        var messages = new ArrayList<>(prompt);
        for (int attempt = 0; ; attempt++) {
            try {
                return validate(model.generate(messages));
            } catch (OllamaUnavailableException e) {
                throw e;
            } catch (Exception e) {
                if (attempt >= maxAttempts - 1) {
                    if (e instanceof IncidentGenerationException incidentError) throw incidentError;
                    throw new IncidentGenerationException("Не удалось получить корректный ответ этапа «" + name + "»", e);
                }
                var cause = e.getCause() == null ? e : e.getCause();
                messages.add(new IncidentLanguageModelPort.Message("user",
                        "Этап «" + name + "»: предыдущий ответ не прошёл проверку: " + cause.getMessage()
                        + ". Ожидается " + expectedResult + ". Повтори только этот шаг, ответ только JSON."));
            }
        }
    }

    public static final class Chain {
        private final IncidentLanguageModelPort model;

        public Chain(IncidentLanguageModelPort model) {
            this.model = model;
        }

        public <T> T then(GenerationStep<T> step) {
            return step.execute(model);
        }
    }
}
