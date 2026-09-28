package com.simulator112.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OneTimeToken(String token, UUID userId, LocalDateTime expiresAt, boolean used) {
    public OneTimeToken markUsed() {
        return new OneTimeToken(token, userId, expiresAt, true);
    }
}
