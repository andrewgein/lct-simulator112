package com.simulator112.review_service.domain.model;

import java.util.UUID;

public record CriterionResult(UUID id, UUID reviewId, String incidentId, int incidentOrder,
                              String criterionName, int score, int maxScore, String feedback) {
    public CriterionResult(String incidentId, int incidentOrder, String criterionName,
                           int score, int maxScore, String feedback) {
        this(null, null, incidentId, incidentOrder, criterionName, score, maxScore, feedback);
    }
}
