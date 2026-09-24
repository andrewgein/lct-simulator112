package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Review(UUID contextId, UUID userId, UUID assignmentId, ReviewStatus status,
                     List<CriterionResult> results,
                     int automaticScore, Integer finalScore, int maxScore,
                     long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                     UUID expertId, String expertComment, Instant confirmedAt,
                     Instant createdAt, Instant updatedAt) {
    public Review {
        results = List.copyOf(results);
    }

    public Review confirm(UUID reviewerId, Integer correctedScore, String comment, Instant confirmedAt) {
        int score = correctedScore == null
                ? (finalScore == null ? automaticScore : finalScore)
                : correctedScore;
        if (score < 0 || score > maxScore) {
            throw new IllegalArgumentException("Итоговый балл должен быть от 0 до " + maxScore);
        }
        return new Review(contextId, userId, assignmentId, ReviewStatus.DONE, results,
                automaticScore, score, maxScore, durationSeconds, timeLimitSeconds, overtimeSeconds,
                reviewerId, comment == null ? null : comment.trim(), confirmedAt, createdAt, updatedAt);
    }
}
