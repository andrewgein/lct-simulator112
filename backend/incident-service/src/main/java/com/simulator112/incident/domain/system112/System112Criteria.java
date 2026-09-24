package com.simulator112.incident.domain.system112;

import com.simulator112.incident.domain.common.DialogueCriterion;

import java.util.List;

public record System112Criteria(List<DialogueCriterion> dialogueCriteria) {
    public System112Criteria {
        dialogueCriteria = List.copyOf(dialogueCriteria);
        validateBudget(dialogueCriteria);
    }

    private static void validateBudget(List<DialogueCriterion> criteria) {
        int budget = criteria.stream().mapToInt(DialogueCriterion::weight).sum();
        if (budget > 40) {
            throw new IllegalArgumentException("Суммарный вес критериев оценки диалога не может превышать 40 баллов");
        }
    }
}
