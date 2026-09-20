package com.simulator112.profileservice.domain.exception;

import java.util.UUID;

public class ProfessionalProfileNotAssignedException extends RuntimeException {

    public ProfessionalProfileNotAssignedException(UUID userId) {
        super("Пользователю с id " + userId + " не назначена специализация");
    }
}
