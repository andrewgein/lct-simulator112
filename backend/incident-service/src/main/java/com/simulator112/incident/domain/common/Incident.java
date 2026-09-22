package com.simulator112.incident.domain.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public interface Incident {
    UUID id();

    String title();

    Address address();

    Difficulty difficulty();

    List<? extends IncidentStage> stages();

    @JsonProperty("targetType")
    IncidentTargetType targetType();
}
