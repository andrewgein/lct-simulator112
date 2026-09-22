package com.simulator112.incident.application.port.out;

import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.Level;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LevelRepository {
    Level save(Level level);

    Optional<Level> findById(UUID levelId);

    List<Level> findAll();

    List<Level> findAllByTargetType(IncidentTargetType targetType);
}
