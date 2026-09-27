package com.simulator112.notification.application.port.in;

import java.util.UUID;

public interface ProcessAccountEventsUseCase {
    void sendPasswordReset(UUID userId, String email, String resetLink);

    void sendEmailVerification(UUID userId, String email, String verificationLink);

    void registerUser(UUID userId, String email);
}
