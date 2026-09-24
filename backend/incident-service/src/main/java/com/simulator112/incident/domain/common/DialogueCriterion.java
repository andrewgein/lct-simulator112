package com.simulator112.incident.domain.common;

import java.util.UUID;

public record DialogueCriterion(UUID id, String name, String hypothesis, int weight) {
    public DialogueCriterion {
        id = id == null ? UUID.randomUUID() : id;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название критерия оценки диалога обязательно");
        }
        if (hypothesis == null || hypothesis.isBlank()) {
            throw new IllegalArgumentException("Гипотеза критерия оценки диалога обязательна");
        }
        if (weight <= 0 || weight > 40) {
            throw new IllegalArgumentException("Вес критерия оценки диалога должен быть от 1 до 40 баллов");
        }
    }
}
