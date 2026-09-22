package com.simulator112.course.application.port.out;

import java.util.UUID;

public interface IncidentCatalogPort {
    void requireIncident(UUID incidentId);
}
