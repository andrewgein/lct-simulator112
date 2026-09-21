package com.simulator112.review_service.adapter.in.rest.dto;

public record CriterionResultResponse(String incidentId, int incidentOrder, String criterionName,
                                      int score, int maxScore, String feedback) {
}
