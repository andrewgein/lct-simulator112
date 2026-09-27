package com.simulator112.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshSession(String token, UUID userId, LocalDateTime expiresAt, boolean revoked) {
    public RefreshSession revoke() {
        return new RefreshSession(token, userId, expiresAt, true);
    }
}
