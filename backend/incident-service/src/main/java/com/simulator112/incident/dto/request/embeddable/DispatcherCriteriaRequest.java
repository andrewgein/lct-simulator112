package com.simulator112.incident.dto.request.embeddable;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DispatcherCriteriaRequest(

        @NotEmpty(message = "Список обязательных вопросов не может быть пустым")
        List<String> requiredQuestions,

        @NotEmpty(message = "Список ожидаемых действий не может быть пустым")
        List<String> expectedActions,

        List<String> criticalMistakes
) {
}
