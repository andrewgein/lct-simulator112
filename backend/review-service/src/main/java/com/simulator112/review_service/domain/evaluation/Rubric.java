package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.List;

public final class Rubric implements ReviewRubric {
    private final ReviewSubmission.TargetType targetType;
    private final List<Stage> stages;

    Rubric(ReviewSubmission.TargetType targetType, List<Stage> stages) {
        this.targetType = targetType;
        this.stages = List.copyOf(stages);
    }

    @Override
    public boolean supports(ReviewSubmission submission) {
        return submission.targetType() == targetType;
    }

    @Override
    public List<CriterionResult> evaluate(ReviewSubmission submission) {
        if (!supports(submission))
            throw new IllegalArgumentException("Рубрика не поддерживает " + submission.targetType());
        return stages.stream().flatMap(stage -> stage.evaluate(submission).stream()).toList();
    }

    public List<Stage> stages() {
        return stages;
    }

    public int maxScorePerIncident() {
        return stages.stream().mapToInt(Stage::maxScore).sum();
    }
}
