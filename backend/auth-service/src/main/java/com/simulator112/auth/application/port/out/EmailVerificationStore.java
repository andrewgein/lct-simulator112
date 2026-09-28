package com.simulator112.auth.application.port.out;

import com.simulator112.auth.domain.model.OneTimeToken;
import java.util.Optional;

public interface EmailVerificationStore {
    Optional<OneTimeToken> findByToken(String token);
    void save(OneTimeToken token);
}
