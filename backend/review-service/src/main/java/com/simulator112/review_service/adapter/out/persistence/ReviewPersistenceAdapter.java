package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.entity.CriterionResultJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements ReviewStore {
    private final SpringDataReviewRepository repository;

    private static ReviewJpaEntity toEntity(Review source) {
        ReviewJpaEntity target = new ReviewJpaEntity();
        target.setContextId(source.contextId());
        target.setUserId(source.userId());
        target.setAssignmentId(source.assignmentId());
        target.setStatus(source.status());
        target.setResults(source.results().stream().map(ReviewPersistenceAdapter::toEntity).toList());
        target.setCreatedAt(source.createdAt());
        target.setUpdatedAt(source.updatedAt());
        return target;
    }

    private static CriterionResultJpaEntity toEntity(CriterionResult source) {
        CriterionResultJpaEntity target = new CriterionResultJpaEntity();
        target.setId(source.id());
        target.setIncidentId(source.incidentId());
        target.setIncidentOrder(source.incidentOrder());
        target.setCriterionName(source.criterionName());
        target.setScore(source.score());
        target.setMaxScore(source.maxScore());
        target.setFeedback(source.feedback());
        return target;
    }

    private static Review toDomain(ReviewJpaEntity source) {
        return new Review(source.getContextId(), source.getUserId(), source.getAssignmentId(), source.getStatus(),
                source.getResults().stream().map(ReviewPersistenceAdapter::toDomain).toList(),
                source.getCreatedAt(), source.getUpdatedAt());
    }

    private static CriterionResult toDomain(CriterionResultJpaEntity source) {
        return new CriterionResult(source.getId(), source.getReview().getContextId(), source.getIncidentId(),
                source.getIncidentOrder(), source.getCriterionName(), source.getScore(), source.getMaxScore(),
                source.getFeedback());
    }

    @Override
    public Review save(Review review) {
        return toDomain(repository.save(toEntity(review)));
    }

    @Override
    public Optional<Review> findByContextId(UUID contextId) {
        return repository.findById(contextId).map(ReviewPersistenceAdapter::toDomain);
    }

    @Override
    public List<Review> findByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReviewPersistenceAdapter::toDomain).toList();
    }
}
