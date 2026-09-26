package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record ReviewAnalyticsResponse(Summary summary, List<TrendPoint> trend, List<CriterionStat> criteria,
                                      List<StudentCriterionStat> heatmap, List<ScenarioStat> scenarios, ReviewPage reviews) {
    public record Summary(long attempts, long students, Integer averageScore, Long averageDurationSeconds,
                          long overtimeAttempts) {
    }

    public record TrendPoint(LocalDate date, int averageScore) {
    }

    public record CriterionStat(String name, long assessments, long score, long maxScore) {
    }

    public record StudentCriterionStat(java.util.UUID userId, String criterion, long score, long maxScore) {
    }

    public record ScenarioStat(String incidentId, int incidentOrder, long attempts, long score, long maxScore) {
    }

    public record ReviewPage(List<ReviewResponse> items, int page, int size, long totalItems, int totalPages) {
    }
}
