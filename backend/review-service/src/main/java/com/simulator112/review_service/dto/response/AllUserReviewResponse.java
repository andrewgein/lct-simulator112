package com.simulator112.review_service.dto.response;

import java.util.List;

import com.simulator112.review_service.model.entity.Review;

public record AllUserReviewResponse (
    List<ReviewResponse> reviews
) {}
