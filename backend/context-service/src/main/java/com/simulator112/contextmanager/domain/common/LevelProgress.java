package com.simulator112.contextmanager.domain.common;

import java.util.List;
import java.util.UUID;

public record LevelProgress(
        UUID contextId,
        IncidentTargetType targetType,
        ExecutionMode executionMode,
        ContextStatus status,
        List<IncidentProgress> incidents
) {
    public LevelProgress {
        incidents = List.copyOf(incidents);
    }
}
