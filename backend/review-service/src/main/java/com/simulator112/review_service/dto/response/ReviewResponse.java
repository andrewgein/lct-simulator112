package com.simulator112.review_service.dto.response;

import com.simulator112.review_service.model.enums.ReviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID uuid,
        String levelId,
        Instant createdAt,
        List<CriterionResultDto> criterionResults,
        String reviewStatus
) {}
