package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.level.Level;

import java.util.List;

public interface ImportIncidentsUseCase {
    ImportedIncidents importIncidents(List<Incident> incidents, boolean createLevels);

    record ImportedIncidents(List<Incident> incidents, List<Level> levels) {
        public ImportedIncidents {
            incidents = List.copyOf(incidents);
            levels = List.copyOf(levels);
        }
    }
}
