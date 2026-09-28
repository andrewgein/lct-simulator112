package com.simulator112.auth.application.port.out;

import java.util.UUID;

public interface AuthEventPublisher {
    void emailVerificationRequested(UUID userId, String email, String link);
    void passwordResetRequested(UUID userId, String email, String link);
    void userCreated(UUID userId, String email);
    void roleChanged(UUID actorUserId, String actorEmail, String actorRole,
                     UUID userId, String previousRole, String newRole);
}
