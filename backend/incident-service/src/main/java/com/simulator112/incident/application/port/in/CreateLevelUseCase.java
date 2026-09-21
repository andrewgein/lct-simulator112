package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.level.Level;

public interface CreateLevelUseCase {
    Level createLevel(Level level);
}
