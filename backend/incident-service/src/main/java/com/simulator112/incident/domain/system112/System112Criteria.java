package com.simulator112.incident.domain.system112;

import java.util.List;

public record System112Criteria(
        List<String> requiredQuestions,
        List<String> expectedActions,
        List<String> criticalMistakes) {
    public System112Criteria {
        requiredQuestions = List.copyOf(requiredQuestions);
        expectedActions = List.copyOf(expectedActions);
        criticalMistakes = criticalMistakes == null ? List.of() : List.copyOf(criticalMistakes);
    }
}
