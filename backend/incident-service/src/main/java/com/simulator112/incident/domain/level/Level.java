package com.simulator112.incident.domain.level;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import java.util.List;
import java.util.UUID;

public record Level(
        UUID id,
        String title,
        IncidentTargetType targetType,
        Difficulty difficulty,
        ExecutionMode executionMode,
        List<UUID> incidentIds) {
    public Level {
        incidentIds = incidentIds == null ? List.of() : List.copyOf(incidentIds);
    }
}
