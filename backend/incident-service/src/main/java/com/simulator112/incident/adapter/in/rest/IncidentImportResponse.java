package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;

import java.util.List;
import java.util.UUID;

public record IncidentImportResponse(List<ImportedIncident> incidents, List<ImportedLevel> levels) {

    public record ImportedIncident(UUID id, String title, IncidentTargetType targetType, Difficulty difficulty) {
    }

    public record ImportedLevel(UUID id, String title, IncidentTargetType targetType, Difficulty difficulty,
                                List<UUID> incidentIds) {
    }
}
