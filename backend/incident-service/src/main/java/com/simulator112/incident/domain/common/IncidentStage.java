package com.simulator112.incident.domain.common;

import java.util.List;
import java.util.UUID;

public interface IncidentStage {
    UUID id();

    String title();

    String description();

    List<CallScenario> calls();
}
