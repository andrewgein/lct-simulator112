package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;

public record UserReviewsResponse(List<ReviewResponse> reviews) {
    public UserReviewsResponse {
        reviews = List.copyOf(reviews);
    }
}
