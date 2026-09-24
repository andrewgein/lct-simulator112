package com.simulator112.review_service.application.exception;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(UUID contextId) {
        super("Проверка не найдена: " + contextId);
    }
}
