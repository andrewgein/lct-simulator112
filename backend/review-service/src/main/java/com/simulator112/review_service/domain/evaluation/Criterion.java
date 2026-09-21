package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.List;

public abstract class Criterion {
    private final String name;
    private final int maxScore;

    protected Criterion(String name, int maxScore) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Название критерия обязательно");
        if (maxScore <= 0) throw new IllegalArgumentException("Максимальный балл должен быть положительным");
        this.name = name;
        this.maxScore = maxScore;
    }

    public String name() {
        return name;
    }

    public int maxScore() {
        return maxScore;
    }

    public abstract List<CriterionResult> evaluate(ReviewSubmission submission);

    protected CriterionResult result(ReviewSubmission.IncidentScenario incident, int score, String feedback) {
        if (score < 0 || score > maxScore) throw new IllegalArgumentException("Балл критерия вне диапазона");
        return new CriterionResult(incident.id(), incident.order(), name, score, maxScore, feedback);
    }
}
