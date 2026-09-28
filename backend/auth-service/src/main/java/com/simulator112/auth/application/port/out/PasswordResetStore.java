package com.simulator112.auth.application.port.out;

import com.simulator112.auth.domain.model.OneTimeToken;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetStore {
    Optional<OneTimeToken> findByToken(String token);
    void deleteAllByUserId(UUID userId);
    void save(OneTimeToken token);
}
