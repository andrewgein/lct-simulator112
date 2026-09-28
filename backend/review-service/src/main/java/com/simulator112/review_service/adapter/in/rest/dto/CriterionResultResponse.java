package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.UUID;

public record CriterionResultResponse(UUID id, String incidentId, int incidentOrder, String criterionName,
                                      int score, int maxScore, String feedback) {
}
