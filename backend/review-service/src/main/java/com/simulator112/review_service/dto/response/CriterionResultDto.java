package com.simulator112.review_service.dto.response;

public record CriterionResultDto (
        String incidentId,
        Integer incidentOrder,
        String criterionName,
        Integer score,
        Integer maxScore,
        String feedback
) {}
