package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.CachedRecommendations;

import java.util.Optional;
import java.util.UUID;

public interface RecommendationStore {
    Optional<CachedRecommendations> findByUserId(UUID userId);

    CachedRecommendations save(CachedRecommendations recommendations);
}
