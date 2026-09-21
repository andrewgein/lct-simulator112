package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.common.Incident;

public interface CreateIncidentUseCase {
    Incident createIncident(Incident incident);
}
