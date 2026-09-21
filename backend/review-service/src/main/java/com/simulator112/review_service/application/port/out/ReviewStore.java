package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewStore {
    Review save(Review review);

    Optional<Review> findByContextId(UUID contextId);

    List<Review> findByUserId(UUID userId);
}
