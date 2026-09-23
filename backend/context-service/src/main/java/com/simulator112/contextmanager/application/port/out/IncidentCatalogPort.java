package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import java.util.UUID;

public interface IncidentCatalogPort {
    IncidentSnapshot getIncident(UUID incidentId, int position);
}
