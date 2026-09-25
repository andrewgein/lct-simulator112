package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import com.simulator112.review_service.application.port.in.AddReviewCommentUseCase;
import com.simulator112.review_service.application.port.in.GetReviewCommentsUseCase;
import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.application.port.out.ReviewCommentNotificationPort;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewCommentApplicationService implements AddReviewCommentUseCase, GetReviewCommentsUseCase {
    private final ReviewStore reviewStore;
    private final ReviewCommentStore commentStore;
    private final ReviewCommentNotificationPort notificationPort;

    @Override
    @Transactional
    public ReviewComment add(UUID contextId, UUID authorId, String text) {
        Review review = requireReview(contextId);
        ReviewComment comment = commentStore.save(ReviewComment.create(contextId, authorId, text, Instant.now()));
        notificationPort.publish(comment, review.userId());
        return comment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewComment> getByContextId(UUID contextId) {
        requireReview(contextId);
        return commentStore.findByContextId(contextId);
    }

    private Review requireReview(UUID contextId) {
        return reviewStore.findByContextId(contextId).orElseThrow(() -> new ReviewNotFoundException(contextId));
    }
}
