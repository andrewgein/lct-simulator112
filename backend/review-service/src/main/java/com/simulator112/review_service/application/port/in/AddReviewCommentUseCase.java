package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.ReviewComment;

import java.util.UUID;

public interface AddReviewCommentUseCase {
    ReviewComment add(UUID contextId, UUID authorId, String authorRole, String text);
}
