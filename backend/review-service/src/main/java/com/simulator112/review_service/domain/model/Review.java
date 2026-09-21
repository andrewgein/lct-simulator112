package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Review(UUID contextId, UUID userId, UUID levelId, ReviewStatus status,
                     List<CriterionResult> results, Instant createdAt, Instant updatedAt) {
    public Review {
        results = List.copyOf(results);
    }

    public Review completed() {
        return new Review(contextId, userId, levelId, ReviewStatus.DONE, results, createdAt, updatedAt);
    }
}
