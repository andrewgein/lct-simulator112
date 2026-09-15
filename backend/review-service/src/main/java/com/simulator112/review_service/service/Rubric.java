package com.simulator112.review_service.service;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.review_service.model.entity.CriterionResult;
import com.simulator112.review_service.config.rubric.LevelReview;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Rubric {
    private final List<Stage> stages;
    public List<CriterionResult> evaluate(FullContext context) {
        if (context.hasLevelContext() || context.getSolutionContextRevisionsCount() > 0) {
            return LevelReview.evaluate(context);
        }
        List<CriterionResult> results = stages.stream()
                .flatMap(stage -> stage.evaluate(context).stream())
                .collect(Collectors.toList());
        // Legacy payloads have no call/action data: normalize their field rubric to 100.
        int total = results.stream().mapToInt(CriterionResult::getMaxScore).sum();
        int allocated = 0;
        int cumulative = 0;
        for (CriterionResult result : results) {
            boolean correct = result.getScore().equals(result.getMaxScore());
            cumulative += result.getMaxScore();
            int next = (int) Math.round(100.0 * cumulative / total);
            result.setMaxScore(next - allocated);
            result.setScore(correct ? next - allocated : 0);
            allocated = next;
        }
        return results;
    }
}
