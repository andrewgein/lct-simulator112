package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.Review;

import java.util.List;
import java.util.UUID;

public interface GetReviewUseCase {
    Review getByContextId(UUID contextId);

    List<Review> getByUserId(UUID userId);
}
