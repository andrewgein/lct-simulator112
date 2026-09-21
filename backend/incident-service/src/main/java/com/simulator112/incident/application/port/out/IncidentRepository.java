package com.simulator112.incident.application.port.out;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository {
    Incident save(Incident incident);

    Optional<Incident> findById(UUID incidentId);

    List<Incident> findAvailable(IncidentTargetType targetType, Difficulty difficulty);

    void deleteById(UUID incidentId);
}
