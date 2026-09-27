package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Review(UUID contextId, UUID userId, UUID assignmentId, ReviewStatus status,
                     List<CriterionResult> results,
                     int automaticScore, Integer finalScore, int maxScore,
                     long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                     UUID expertId, String expertComment, Instant confirmedAt,
                     Instant createdAt, Instant updatedAt,
                     Integer threshold3, Integer threshold4, Integer threshold5) {
    public Review(UUID contextId, UUID userId, UUID assignmentId, ReviewStatus status,
                  List<CriterionResult> results, int automaticScore, Integer finalScore, int maxScore,
                  long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                  UUID expertId, String expertComment, Instant confirmedAt, Instant createdAt, Instant updatedAt) {
        this(contextId, userId, assignmentId, status, results, automaticScore, finalScore, maxScore,
                durationSeconds, timeLimitSeconds, overtimeSeconds, expertId, expertComment, confirmedAt,
                createdAt, updatedAt, null, null, null);
    }

    public Review {
        results = List.copyOf(results);
        if ((threshold3 == null && (threshold4 != null || threshold5 != null))
                || (threshold3 != null && (threshold4 == null || threshold5 == null
                || threshold3 < 0 || threshold4 <= threshold3 || threshold5 <= threshold4 || threshold5 > 100))) {
            throw new IllegalArgumentException("Некорректные пороги оценки задания");
        }
    }

    public Integer grade() {
        return finalScore == null ? null : gradeFor(finalScore, maxScore, threshold3, threshold4, threshold5);
    }

    public static Integer gradeFor(int score, int maxScore, Integer threshold3, Integer threshold4, Integer threshold5) {
        if (threshold3 == null) return null;
        if (maxScore <= 0 || score < 0 || score > maxScore) {
            throw new IllegalStateException("Некорректный итоговый балл задания");
        }
        long percent = (long) score * 100;
        if (percent >= (long) threshold5 * maxScore) return 5;
        if (percent >= (long) threshold4 * maxScore) return 4;
        if (percent >= (long) threshold3 * maxScore) return 3;
        return 2;
    }

    public Boolean passed() {
        Integer grade = grade();
        return grade == null ? null : grade >= 3;
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
                reviewerId, comment == null ? null : comment.trim(), confirmedAt, createdAt, updatedAt,
                threshold3, threshold4, threshold5);
    }
}
