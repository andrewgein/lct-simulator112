package com.simulator112.review_service.adapter.in.rest.dto;

public record ErrorStatisticResponse(String criterionName, int attempts, int failedCount,
                                     int scoreEarned, int scoreMax, double errorRate) {
}
