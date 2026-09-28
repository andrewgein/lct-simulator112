package com.simulator112.auth.application.port.in;

import java.util.UUID;

public interface ValidateSessionUseCase {
    boolean isSessionValid(UUID userId, String role);
}
