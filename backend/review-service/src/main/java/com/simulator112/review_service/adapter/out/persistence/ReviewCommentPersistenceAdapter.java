package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.entity.ReviewCommentJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewCommentRepository;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.domain.model.ReviewComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewCommentPersistenceAdapter implements ReviewCommentStore {
    private final SpringDataReviewCommentRepository commentRepository;
    private final SpringDataReviewRepository reviewRepository;

    @Override
    public ReviewComment save(ReviewComment comment) {
        ReviewCommentJpaEntity entity = new ReviewCommentJpaEntity();
        entity.setId(comment.id());
        entity.setReview(reviewRepository.getReferenceById(comment.reviewContextId()));
        entity.setAuthorId(comment.authorId());
        entity.setText(comment.text());
        entity.setCreatedAt(comment.createdAt());
        return toDomain(commentRepository.save(entity));
    }

    @Override
    public List<ReviewComment> findByContextId(UUID contextId) {
        return commentRepository.findAllByReviewContextIdOrderByCreatedAtAsc(contextId).stream()
                .map(ReviewCommentPersistenceAdapter::toDomain).toList();
    }

    private static ReviewComment toDomain(ReviewCommentJpaEntity source) {
        return new ReviewComment(source.getId(), source.getReview().getContextId(), source.getAuthorId(),
                source.getText(), source.getCreatedAt());
    }
}
