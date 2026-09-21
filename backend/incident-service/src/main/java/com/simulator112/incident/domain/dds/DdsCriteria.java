package com.simulator112.incident.domain.dds;

import java.util.List;

public record DdsCriteria(
        List<String> requiredQuestions,
        List<String> expectedActions,
        List<String> criticalMistakes) {
    public DdsCriteria {
        requiredQuestions = List.copyOf(requiredQuestions);
        expectedActions = List.copyOf(expectedActions);
        criticalMistakes = criticalMistakes == null ? List.of() : List.copyOf(criticalMistakes);
    }
}
