package com.simulator112.contextmanager.domain.common;

import java.util.List;

public record Criteria(List<String> requiredQuestions, List<String> expectedActions, List<String> criticalMistakes) {
    public Criteria {
        requiredQuestions = List.copyOf(requiredQuestions);
        expectedActions = List.copyOf(expectedActions);
        criticalMistakes = List.copyOf(criticalMistakes);
    }
}
