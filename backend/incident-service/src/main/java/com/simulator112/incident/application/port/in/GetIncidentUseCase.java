package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.common.Incident;

import java.util.UUID;

public interface GetIncidentUseCase {
    Incident getIncident(UUID incidentId);
}
