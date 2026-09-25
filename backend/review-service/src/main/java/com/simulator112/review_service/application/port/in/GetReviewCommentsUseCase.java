package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.ReviewComment;

import java.util.List;
import java.util.UUID;

public interface GetReviewCommentsUseCase {
    List<ReviewComment> getByContextId(UUID contextId);
}
