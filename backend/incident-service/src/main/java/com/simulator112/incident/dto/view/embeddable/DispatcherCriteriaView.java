package com.simulator112.incident.dto.view.embeddable;

import java.util.List;

public record DispatcherCriteriaView(
        List<String> requiredQuestions,
        List<String> expectedActions,
        List<String> criticalMistakes
) {
}
