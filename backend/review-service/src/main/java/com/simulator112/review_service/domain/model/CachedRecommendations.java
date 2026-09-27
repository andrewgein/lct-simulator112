package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CachedRecommendations(UUID userId, List<String> recommendations, int reviewsCount, Instant generatedAt) {
    public CachedRecommendations {
        recommendations = List.copyOf(recommendations);
    }
}
