package com.simulator112.review_service.mapper;

import com.simulator112.review_service.dto.response.AllUserReviewResponse;
import com.simulator112.review_service.dto.response.CriterionResultDto;
import com.simulator112.review_service.dto.response.ReviewResponse;
import com.simulator112.review_service.model.entity.CriterionResult;
import com.simulator112.review_service.model.entity.Review;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewMapper {
    private ReviewMapper() {}
    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getContextId(),
                review.getLevelId(),
                review.getCreatedAt(),
                review.getResult().stream().map(ReviewMapper::toDto).collect(Collectors.toList()),
                review.getStatus().toString()
        );
    }

    public static CriterionResultDto toDto(CriterionResult criterion) {
        return new CriterionResultDto(
                criterion.getIncidentId(),
                criterion.getIncidentOrder(),
                criterion.getCriterionName(),
                criterion.getScore(),
                criterion.getMaxScore(),
                criterion.getFeedback()
        );
    }

    public static AllUserReviewResponse toResponse(List<Review> reviews) {
        return new AllUserReviewResponse(
                    reviews.stream().map(ReviewMapper::toResponse).collect(Collectors.toList())
                    );
    }
}
