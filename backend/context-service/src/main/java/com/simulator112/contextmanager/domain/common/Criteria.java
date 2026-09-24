package com.simulator112.contextmanager.domain.common;

import java.util.List;

public record Criteria(List<DialogueCriterion> dialogueCriteria) {
    public Criteria {
        dialogueCriteria = List.copyOf(dialogueCriteria);
        int budget = dialogueCriteria.stream().mapToInt(DialogueCriterion::weight).sum();
        if (budget > 40) {
            throw new IllegalArgumentException("Суммарный вес критериев оценки диалога не может превышать 40 баллов");
        }
    }
}
