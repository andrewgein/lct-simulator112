package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.Level;
import java.util.List;
import java.util.UUID;

public interface GetLevelUseCase {
    Level getLevel(UUID levelId);

    List<Level> getLevels(IncidentTargetType targetType);
}
