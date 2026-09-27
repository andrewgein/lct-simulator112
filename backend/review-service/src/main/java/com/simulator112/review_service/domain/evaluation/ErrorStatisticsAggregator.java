package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ErrorStatistic;
import com.simulator112.review_service.domain.model.Review;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ErrorStatisticsAggregator {
    private ErrorStatisticsAggregator() {
    }

    public static List<ErrorStatistic> aggregate(List<Review> reviews) {
        Map<String, int[]> byCriterion = new LinkedHashMap<>();
        for (Review review : reviews) {
            for (CriterionResult result : review.results()) {
                int[] totals = byCriterion.computeIfAbsent(result.criterionName(), name -> new int[4]);
                totals[0]++;
                if (result.score() < result.maxScore()) totals[1]++;
                totals[2] += result.score();
                totals[3] += result.maxScore();
            }
        }
        return byCriterion.entrySet().stream()
                .map(entry -> new ErrorStatistic(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        entry.getValue()[2], entry.getValue()[3]))
                .sorted(Comparator.comparingDouble(ErrorStatistic::errorRate).reversed())
                .toList();
    }
}
