package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.entity.CachedRecommendationsJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataCachedRecommendationsRepository;
import com.simulator112.review_service.application.port.out.RecommendationStore;
import com.simulator112.review_service.domain.model.CachedRecommendations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CachedRecommendationsPersistenceAdapter implements RecommendationStore {
    private static final String SEPARATOR = "\n";

    private final SpringDataCachedRecommendationsRepository repository;

    @Override
    public Optional<CachedRecommendations> findByUserId(UUID userId) {
        return repository.findById(userId).map(CachedRecommendationsPersistenceAdapter::toDomain);
    }

    @Override
    public CachedRecommendations save(CachedRecommendations recommendations) {
        CachedRecommendationsJpaEntity entity = new CachedRecommendationsJpaEntity();
        entity.setUserId(recommendations.userId());
        entity.setRecommendations(String.join(SEPARATOR, recommendations.recommendations()));
        entity.setReviewsCount(recommendations.reviewsCount());
        entity.setGeneratedAt(recommendations.generatedAt());
        return toDomain(repository.save(entity));
    }

    private static CachedRecommendations toDomain(CachedRecommendationsJpaEntity source) {
        List<String> recommendations = source.getRecommendations().isBlank()
                ? List.of() : List.of(source.getRecommendations().split(SEPARATOR));
        return new CachedRecommendations(source.getUserId(), recommendations, source.getReviewsCount(), source.getGeneratedAt());
    }
}
