package com.simulator112.review_service.service;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.review_service.model.entity.CriterionResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class Criterion {
    protected final String name;
    @Getter
    protected final int maxScore;

    public abstract List<CriterionResult> evaluate(FullContext context);

    protected CriterionResult resultOf(Boolean expression, String onCorrect, String onIncorrect, Integer maxScore) {
        CriterionResult result = new CriterionResult();
        result.setCriterionName(this.name);
        result.setMaxScore(maxScore);
        result.setFeedback(expression ? onCorrect : onIncorrect);
        result.setScore(expression ? maxScore : 0);
        return result;
    }
}
