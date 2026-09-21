package com.simulator112.contextmanager.domain.common;

import com.simulator112.shared.dto.Difficulty;
import java.util.List;
import java.util.UUID;

public record LevelScenario(UUID id, String title, IncidentTargetType targetType, Difficulty difficulty,
                            ExecutionMode executionMode, List<IncidentSnapshot> incidents) {
    public LevelScenario {
        incidents = List.copyOf(incidents);
    }
}
