package com.simulator112.profileservice.domain.exception;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(UUID userId) {
        super("Пользователь с id " + userId + " не найден");
    }
}
