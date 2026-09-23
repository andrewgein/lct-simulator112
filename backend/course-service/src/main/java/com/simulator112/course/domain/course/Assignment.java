package com.simulator112.course.domain.course;

import java.util.List;
import java.util.UUID;

public record Assignment(
        UUID id,
        String title,
        String description,
        AssignmentDifficulty difficulty,
        AssignmentExecutionMode executionMode,
        List<UUID> incidentIds) {
    public Assignment {
        incidentIds = incidentIds == null ? List.of() : List.copyOf(incidentIds);
    }
}
