package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.common.LevelScenario;
import java.util.UUID;

public interface LevelCatalogPort {
    LevelScenario getLevel(UUID levelId);
}
