package com.simulator112.auth.application.port.out;

import com.simulator112.auth.domain.model.OneTimeToken;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationStore {
    Optional<OneTimeToken> findByToken(String token);
    void save(OneTimeToken token);
    void deleteAllByUserId(UUID userId);
}
