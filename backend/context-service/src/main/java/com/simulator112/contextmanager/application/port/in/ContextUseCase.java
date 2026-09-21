package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import java.util.UUID;

public interface ContextUseCase {
    UUID createContext(UUID userId, UUID levelId);

    IncidentSnapshot getIncidentContext(String contextId);

    void closeContext(UUID contextId);
}
