package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(UUID contextId, UUID userId, UUID assignmentId, Instant createdAt,
                             List<CriterionResultResponse> criterionResults,
                             int automaticScore, Integer finalScore, int maxScore,
                             long durationSeconds, long timeLimitSeconds, long overtimeSeconds,
                             UUID expertId, String expertComment, Instant confirmedAt,
                             String status, Integer grade, Boolean passed) {
}
