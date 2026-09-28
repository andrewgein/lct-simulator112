package com.simulator112.auth.application.port.out;

import com.simulator112.auth.domain.model.RefreshSession;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionStore {
    Optional<RefreshSession> findByToken(String token);
    void deleteAllByUserId(UUID userId);
    void save(RefreshSession token);
}
