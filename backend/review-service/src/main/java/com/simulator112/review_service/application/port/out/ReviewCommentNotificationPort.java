package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.ReviewComment;

import java.util.UUID;

public interface ReviewCommentNotificationPort {
    void publish(ReviewComment comment, UUID recipientUserId);
}
