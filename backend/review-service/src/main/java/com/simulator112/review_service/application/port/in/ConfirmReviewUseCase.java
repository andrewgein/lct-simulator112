package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.Review;

import java.util.UUID;

public interface ConfirmReviewUseCase {
    Review confirm(UUID contextId, UUID expertId, Integer finalScore, String comment);
}
