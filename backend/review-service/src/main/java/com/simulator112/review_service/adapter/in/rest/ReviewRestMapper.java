package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.CriterionResultResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewAnalyticsResponse;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewAnalytics;

final class ReviewRestMapper {
    private ReviewRestMapper() {
    }

    static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.contextId(), review.userId(), review.assignmentId(), review.createdAt(),
                review.results().stream().map(value -> new CriterionResultResponse(value.incidentId(),
                                value.incidentOrder(), value.criterionName(), value.score(), value.maxScore(), value.feedback()))
                        .toList(),
                review.automaticScore(), review.finalScore(), review.maxScore(), review.durationSeconds(),
                review.timeLimitSeconds(), review.overtimeSeconds(), review.expertId(), review.expertComment(),
                review.confirmedAt(), review.status().name());
    }

    static ReviewAnalyticsResponse toResponse(ReviewAnalytics analytics) {
        var summary = analytics.summary();
        var page = analytics.reviews();
        return new ReviewAnalyticsResponse(
                new ReviewAnalyticsResponse.Summary(summary.attempts(), summary.students(), summary.averageScore(),
                        summary.averageDurationSeconds(), summary.overtimeAttempts()),
                analytics.trend().stream().map(value -> new ReviewAnalyticsResponse.TrendPoint(
                        value.date(), value.averageScore())).toList(),
                analytics.criteria().stream().map(value -> new ReviewAnalyticsResponse.CriterionStat(
                        value.name(), value.assessments(), value.score(), value.maxScore())).toList(),
                analytics.heatmap().stream().map(value -> new ReviewAnalyticsResponse.StudentCriterionStat(
                        value.userId(), value.criterion(), value.score(), value.maxScore())).toList(),
                analytics.scenarios().stream().map(value -> new ReviewAnalyticsResponse.ScenarioStat(
                        value.incidentId(), value.incidentOrder(), value.attempts(), value.score(), value.maxScore())).toList(),
                new ReviewAnalyticsResponse.ReviewPage(page.items().stream().map(ReviewRestMapper::toResponse).toList(),
                        page.page(), page.size(), page.totalItems(), page.totalPages()));
    }
}
