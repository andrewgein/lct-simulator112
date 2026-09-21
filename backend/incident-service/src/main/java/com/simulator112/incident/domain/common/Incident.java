package com.simulator112.incident.domain.common;

import java.util.List;
import java.util.UUID;

public interface Incident {
    UUID id();

    String title();

    Address address();

    Difficulty difficulty();

    List<? extends IncidentStage> stages();

    IncidentTargetType targetType();
}
