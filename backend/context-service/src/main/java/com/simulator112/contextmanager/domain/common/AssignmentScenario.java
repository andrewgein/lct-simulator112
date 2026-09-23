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
        List<IncidentSnapshot> incidents) {
    public AssignmentScenario {
        incidents = List.copyOf(incidents);
    }
}
