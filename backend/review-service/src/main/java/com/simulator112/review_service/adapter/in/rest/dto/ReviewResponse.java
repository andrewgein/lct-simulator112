package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(UUID contextId, UUID levelId, Instant createdAt,
                             List<CriterionResultResponse> criterionResults, String status) {
}
