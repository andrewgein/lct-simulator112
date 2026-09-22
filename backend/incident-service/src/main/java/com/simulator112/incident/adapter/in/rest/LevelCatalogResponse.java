package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.ExecutionMode;
import java.util.UUID;

public record LevelCatalogResponse(
        UUID id,
        String title,
        IncidentTargetType targetType,
        Difficulty difficulty,
        ExecutionMode executionMode,
        int incidentCount) {
}
