package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Review(UUID contextId, UUID userId, UUID assignmentId, ReviewStatus status,
                     List<CriterionResult> results,
                     int automaticScore, Integer finalScore, int maxScore,
                     long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                     UUID expertId, String expertComment, Instant confirmedAt,
                     Instant createdAt, Instant updatedAt,
                     Integer threshold3, Integer threshold4, Integer threshold5,
                     List<IncidentSummary> incidents, List<DispatcherCardSummary> cards) {
    public Review(UUID contextId, UUID userId, UUID assignmentId, ReviewStatus status,
                  List<CriterionResult> results, int automaticScore, Integer finalScore, int maxScore,
                  long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                  UUID expertId, String expertComment, Instant confirmedAt, Instant createdAt, Instant updatedAt,
                  Integer threshold3, Integer threshold4, Integer threshold5) {
        this(contextId, userId, assignmentId, status, results, automaticScore, finalScore, maxScore,
                durationSeconds, timeLimitSeconds, overtimeSeconds, expertId, expertComment, confirmedAt,
                createdAt, updatedAt, threshold3, threshold4, threshold5, List.of(), List.of());
    }

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
        incidents = List.copyOf(incidents);
        cards = List.copyOf(cards);
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
                threshold3, threshold4, threshold5, incidents, cards);
    }

    public Review updateCriteriaScores(UUID reviewerId, Map<UUID, Integer> corrections, Instant confirmedAt) {
        if (status != ReviewStatus.DONE) {
            throw new IllegalStateException("Автоматическая проверка ещё не завершена");
        }
        var remaining = new HashMap<>(corrections);
        List<CriterionResult> updatedResults = results.stream().map(result -> {
            Integer newScore = remaining.remove(result.id());
            if (newScore == null) return result;
            if (newScore < 0 || newScore > result.maxScore()) {
                throw new IllegalArgumentException("Балл по критерию должен быть от 0 до " + result.maxScore());
            }
            return new CriterionResult(result.id(), result.reviewId(), result.incidentId(), result.incidentOrder(),
                    result.criterionName(), newScore, result.maxScore(), result.feedback());
        }).toList();
        if (!remaining.isEmpty()) {
            throw new IllegalArgumentException("Строка критерия не найдена");
        }
        int score = updatedResults.stream().mapToInt(CriterionResult::score).sum();
        return new Review(contextId, userId, assignmentId, status, updatedResults,
                automaticScore, score, maxScore, durationSeconds, timeLimitSeconds, overtimeSeconds,
                reviewerId, expertComment, confirmedAt, createdAt, updatedAt,
                threshold3, threshold4, threshold5, incidents, cards);
    }
}
