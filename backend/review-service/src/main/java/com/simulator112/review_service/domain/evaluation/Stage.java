package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.List;

public record Stage(String name, List<Criterion> criteria) {
    public Stage {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Название этапа проверки обязательно");
        criteria = List.copyOf(criteria);
        if (criteria.isEmpty()) throw new IllegalArgumentException("Этап проверки должен содержать критерии");
    }

    public int maxScore() {
        return criteria.stream().mapToInt(Criterion::maxScore).sum();
    }

    public List<CriterionResult> evaluate(ReviewSubmission submission) {
        return criteria.stream().flatMap(criterion -> criterion.evaluate(submission).stream()).toList();
    }
}
