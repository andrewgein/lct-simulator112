package com.simulator112.review_service.adapter.out.persistence;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.simulator112.review_service.adapter.out.persistence.entity.CriterionResultJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.DispatcherCardSummary;
import com.simulator112.review_service.domain.model.IncidentSummary;
import com.simulator112.review_service.domain.model.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements ReviewStore {
    private static final TypeReference<List<IncidentSummary>> INCIDENTS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<DispatcherCardSummary>> CARDS_TYPE = new TypeReference<>() {
    };

    private final SpringDataReviewRepository repository;
    private final ObjectMapper objectMapper;

    private ReviewJpaEntity toEntity(Review source) {
        ReviewJpaEntity target = new ReviewJpaEntity();
        target.setContextId(source.contextId());
        target.setUserId(source.userId());
        target.setAssignmentId(source.assignmentId());
        target.setStatus(source.status());
        target.setResults(source.results().stream().map(ReviewPersistenceAdapter::toEntity).toList());
        target.setAutomaticScore(source.automaticScore());
        target.setFinalScore(source.finalScore());
        target.setMaxScore(source.maxScore());
        target.setThreshold3(source.threshold3());
        target.setThreshold4(source.threshold4());
        target.setThreshold5(source.threshold5());
        target.setDurationSeconds(source.durationSeconds());
        target.setTimeLimitSeconds(source.timeLimitSeconds());
        target.setOvertimeSeconds(source.overtimeSeconds());
        target.setExpertId(source.expertId());
        target.setExpertComment(source.expertComment());
        target.setConfirmedAt(source.confirmedAt());
        target.setCreatedAt(source.createdAt());
        target.setUpdatedAt(source.updatedAt());
        target.setIncidentsSnapshot(writeJson(source.incidents()));
        target.setCardsSnapshot(writeJson(source.cards()));
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

    private Review toDomain(ReviewJpaEntity source) {
        return new Review(source.getContextId(), source.getUserId(), source.getAssignmentId(), source.getStatus(),
                source.getResults().stream().map(ReviewPersistenceAdapter::toDomain).toList(),
                source.getAutomaticScore(), source.getFinalScore(), source.getMaxScore(),
                source.getDurationSeconds(), source.getTimeLimitSeconds(), source.getOvertimeSeconds(),
                source.getExpertId(), source.getExpertComment(), source.getConfirmedAt(),
                source.getCreatedAt(), source.getUpdatedAt(),
                source.getThreshold3(), source.getThreshold4(), source.getThreshold5(),
                readJson(source.getIncidentsSnapshot(), INCIDENTS_TYPE),
                readJson(source.getCardsSnapshot(), CARDS_TYPE));
    }

    private static CriterionResult toDomain(CriterionResultJpaEntity source) {
        return new CriterionResult(source.getId(), source.getReview().getContextId(), source.getIncidentId(),
                source.getIncidentOrder(), source.getCriterionName(), source.getScore(), source.getMaxScore(),
                source.getFeedback());
    }

    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private <T> List<T> readJson(String value, TypeReference<List<T>> type) {
        return value == null || value.isBlank() ? List.of() : objectMapper.readValue(value, type);
    }

    @Override
    public Review save(Review review) {
        return toDomain(repository.save(toEntity(review)));
    }

    @Override
    public Optional<Review> findByContextId(UUID contextId) {
        return repository.findById(contextId).map(this::toDomain);
    }

    @Override
    public List<Review> findByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain).toList();
    }
}
