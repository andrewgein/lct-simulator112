package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewCommentForbiddenException;
import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import com.simulator112.review_service.application.port.in.AddReviewCommentUseCase;
import com.simulator112.review_service.application.port.in.GetReviewCommentsUseCase;
import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.application.port.out.ReviewCommentNotificationPort;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.application.port.out.TeacherStudentAccessPort;
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
    private final TeacherStudentAccessPort teacherStudentAccess;

    @Override
    @Transactional
    public ReviewComment add(UUID contextId, UUID authorId, String authorRole, String text) {
        Review review = requireReview(contextId);
        if (!canComment(authorId, authorRole, review.userId())) {
            throw new ReviewCommentForbiddenException();
        }
        ReviewComment comment = commentStore.save(ReviewComment.create(contextId, authorId, text, Instant.now()));
        notificationPort.publish(comment, review.userId());
        return comment;
    }

    private boolean canComment(UUID authorId, String authorRole, UUID studentId) {
        if ("ADMIN".equals(authorRole)) return true;
        if ("SUPERVISOR".equals(authorRole)) return teacherStudentAccess.isStudentOfTeacher(authorId, studentId);
        return false;
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
