package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.CriterionResultResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.domain.model.Review;

final class ReviewRestMapper {
    private ReviewRestMapper() {
    }

    static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.contextId(), review.levelId(), review.createdAt(),
                review.results().stream().map(value -> new CriterionResultResponse(value.incidentId(),
                                value.incidentOrder(), value.criterionName(), value.score(), value.maxScore(), value.feedback()))
                        .toList(), review.status().name());
    }
}
