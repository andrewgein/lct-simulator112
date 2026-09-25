package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.ReviewComment;

import java.util.List;
import java.util.UUID;

public interface ReviewCommentStore {
    ReviewComment save(ReviewComment comment);

    List<ReviewComment> findByContextId(UUID contextId);
}
