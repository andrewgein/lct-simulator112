package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.ArrayList;
import java.util.List;

public final class RubricBuilder {
    private final ReviewSubmission.TargetType targetType;
    private final List<Stage> stages = new ArrayList<>();

    public RubricBuilder(ReviewSubmission.TargetType targetType) {
        this.targetType = targetType;
    }

    public RubricBuilder addStage(Stage stage) {
        stages.add(stage);
        return this;
    }

    public Rubric build() {
        if (stages.isEmpty()) throw new IllegalStateException("Рубрика должна содержать этапы");
        int total = stages.stream().mapToInt(Stage::maxScore).sum();
        if (total != 100)
            throw new IllegalStateException("Сумма баллов рубрики должна быть равна 100, получено: " + total);
        return new Rubric(targetType, stages);
    }
}
