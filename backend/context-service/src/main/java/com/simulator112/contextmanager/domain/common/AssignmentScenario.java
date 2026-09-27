package com.simulator112.contextmanager.domain.common;

import com.simulator112.shared.dto.Difficulty;
import java.util.List;
import java.util.UUID;

public record AssignmentScenario(
        UUID assignmentId,
        UUID userId,
        String title,
        IncidentTargetType targetType,
        Difficulty difficulty,
        ExecutionMode executionMode,
        List<IncidentSnapshot> incidents,
        Integer threshold3,
        Integer threshold4,
        Integer threshold5) {
    public AssignmentScenario(UUID assignmentId, UUID userId, String title, IncidentTargetType targetType,
                              Difficulty difficulty, ExecutionMode executionMode, List<IncidentSnapshot> incidents) {
        this(assignmentId, userId, title, targetType, difficulty, executionMode, incidents, null, null, null);
    }

    public AssignmentScenario {
        incidents = List.copyOf(incidents);
    }
}
