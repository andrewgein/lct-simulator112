package com.simulator112.incident.application.port.in;

import java.util.UUID;

public interface DeleteIncidentUseCase {
    void deleteIncident(UUID incidentId);
}
