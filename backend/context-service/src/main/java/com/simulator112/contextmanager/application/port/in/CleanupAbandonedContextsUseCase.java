package com.simulator112.contextmanager.application.port.in;

import java.time.Instant;

public interface CleanupAbandonedContextsUseCase {
    void cleanupAbandoned(Instant updatedBefore);
}
