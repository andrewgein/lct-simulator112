package com.simulator112.incident.application.port.in;

import com.simulator112.incident.domain.level.Level;
import java.util.UUID;

public interface UpdateLevelUseCase {
    Level updateLevel(UUID levelId, Level level);
}
