package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.port.out.IncidentLanguageModelPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public abstract class JsonResponseStep<T> extends GenerationStep<T> {
    protected final ObjectMapper mapper;

    protected JsonResponseStep(String name, List<IncidentLanguageModelPort.Message> prompt,
            String expectedResult, int maxAttempts, ObjectMapper mapper) {
        super(name, prompt, expectedResult, maxAttempts);
        this.mapper = mapper;
    }

    @Override
    protected final T validate(String response) {
        return validateJson(mapper.readTree(response));
    }

    protected abstract T validateJson(JsonNode response);
}
