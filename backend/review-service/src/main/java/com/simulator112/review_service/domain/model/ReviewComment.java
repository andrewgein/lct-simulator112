package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ReviewComment(UUID id, UUID reviewContextId, UUID authorId, String text, Instant createdAt) {
    public static final int MAX_TEXT_LENGTH = 4000;

    public ReviewComment {
        if (id == null || reviewContextId == null || authorId == null || createdAt == null) {
            throw new IllegalArgumentException("Комментарий должен содержать идентификаторы и дату создания");
        }
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Комментарий не может быть длиннее " + MAX_TEXT_LENGTH + " символов");
        }
    }

    public static ReviewComment create(UUID reviewContextId, UUID authorId, String text, Instant createdAt) {
        return new ReviewComment(UUID.randomUUID(), reviewContextId, authorId, text, createdAt);
    }
}
