package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;

import java.util.List;

public interface FindAvailableIncidentsUseCase {
    List<Incident> findAvailableIncidents(IncidentTargetType targetType, Difficulty difficulty);
}
