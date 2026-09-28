package com.simulator112.review_service.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTests {
    @Test
    void updatesCriterionScoreAndRecalculatesTotal() {
        UUID fieldsId = UUID.randomUUID();
        UUID callsId = UUID.randomUUID();
        Review review = review(List.of(
                new CriterionResult(fieldsId, null, "incident", 1, "Поля", 35, 70, "Проверка"),
                new CriterionResult(callsId, null, "incident", 1, "Обработка звонков", 10, 10, "Проверка")));
        UUID expertId = UUID.randomUUID();

        Review updated = review.updateCriteriaScores(expertId, Map.of(fieldsId, 50), Instant.now());

        assertThat(updated.finalScore()).isEqualTo(60);
        assertThat(updated.automaticScore()).isEqualTo(45);
        assertThat(updated.expertId()).isEqualTo(expertId);
        assertThat(updated.results().stream().filter(result -> result.id().equals(fieldsId)).findFirst()
                .orElseThrow().score()).isEqualTo(50);
    }

    @Test
    void rejectsScoreAboveCriterionMax() {
        UUID fieldsId = UUID.randomUUID();
        Review review = review(List.of(new CriterionResult(fieldsId, null, "incident", 1, "Поля", 35, 70, "Проверка")));

        assertThatThrownBy(() -> review.updateCriteriaScores(UUID.randomUUID(), Map.of(fieldsId, 71), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownCriterionId() {
        Review review = review(List.of(new CriterionResult(UUID.randomUUID(), null, "incident", 1, "Поля", 35, 70, "Проверка")));

        assertThatThrownBy(() -> review.updateCriteriaScores(UUID.randomUUID(), Map.of(UUID.randomUUID(), 10), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Review review(List<CriterionResult> results) {
        return new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ReviewStatus.DONE,
                results, results.stream().mapToInt(CriterionResult::score).sum(),
                results.stream().mapToInt(CriterionResult::score).sum(),
                results.stream().mapToInt(CriterionResult::maxScore).sum(),
                0, 30, 0, null, null, null, Instant.now(), Instant.now());
    }
}
