package com.simulator112.course.domain.course;

import java.util.List;
import java.util.UUID;

public record Assignment(
        UUID id,
        String title,
        String description,
        AssignmentDifficulty difficulty,
        AssignmentExecutionMode executionMode,
        List<UUID> incidentIds,
        Integer threshold3,
        Integer threshold4,
        Integer threshold5) {
    public Assignment {
        incidentIds = incidentIds == null ? List.of() : List.copyOf(incidentIds);
        if ((threshold3 == null || threshold4 == null || threshold5 == null)
                && (threshold3 != null || threshold4 != null || threshold5 != null)) {
            throw new IllegalArgumentException("Необходимо указать пороги для всех оценок 3, 4 и 5");
        }
        if (threshold3 != null && (threshold3 < 0 || threshold3 > 100
                || threshold4 <= threshold3 || threshold4 > 100
                || threshold5 <= threshold4 || threshold5 > 100)) {
            throw new IllegalArgumentException("Пороги оценок должны возрастать и находиться в диапазоне 0–100%");
        }
    }

    public Assignment(UUID id, String title, String description, AssignmentDifficulty difficulty,
                      AssignmentExecutionMode executionMode, List<UUID> incidentIds) {
        this(id, title, description, difficulty, executionMode, incidentIds, null, null, null);
    }
}
