package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.ExecutionMode;
import java.util.List;
import java.util.UUID;

public record AdminLevelResponse(
        UUID id,
        String title,
        IncidentTargetType targetType,
        Difficulty difficulty,
        ExecutionMode executionMode,
        List<Incident> incidents) {
}
