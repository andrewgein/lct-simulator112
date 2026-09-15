package com.simulator112.incident.dto.view;

import com.simulator112.incident.model.enums.Difficulty;
import java.util.List;
import java.util.UUID;

public record LevelView(
    UUID id,
    String title,
    Difficulty difficulty,
    List<IncidentFullView> incidents
) {
}
