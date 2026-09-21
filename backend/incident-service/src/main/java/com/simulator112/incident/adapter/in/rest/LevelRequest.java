package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.ExecutionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record LevelRequest(
        @NotBlank String title,
        @NotNull IncidentTargetType targetType,
        @NotNull Difficulty difficulty,
        @NotNull ExecutionMode executionMode,
        @NotEmpty List<@NotNull UUID> incidentIds) {
}
