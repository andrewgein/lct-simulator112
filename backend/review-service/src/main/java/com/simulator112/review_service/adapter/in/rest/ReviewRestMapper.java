package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.CriterionResultResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ErrorStatisticResponse;
import com.simulator112.review_service.adapter.in.rest.dto.PersonalStatisticsResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.domain.model.PersonalStatistics;
import com.simulator112.review_service.domain.model.Review;

final class ReviewRestMapper {
    private ReviewRestMapper() {
    }

    static PersonalStatisticsResponse toResponse(PersonalStatistics statistics) {
        return new PersonalStatisticsResponse(statistics.errorStatistics().stream()
                .map(value -> new ErrorStatisticResponse(value.criterionName(), value.attempts(), value.failedCount(),
                        value.scoreEarned(), value.scoreMax(), value.errorRate()))
                .toList(), statistics.recommendations());
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
}
