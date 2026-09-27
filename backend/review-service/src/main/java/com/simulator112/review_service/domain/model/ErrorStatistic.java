package com.simulator112.review_service.domain.model;

public record ErrorStatistic(String criterionName, int attempts, int failedCount, int scoreEarned, int scoreMax) {
    public double errorRate() {
        return attempts == 0 ? 0 : (double) failedCount / attempts;
    }
}
