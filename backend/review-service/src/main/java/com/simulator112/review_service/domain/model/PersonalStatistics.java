package com.simulator112.review_service.domain.model;

import java.util.List;

public record PersonalStatistics(List<ErrorStatistic> errorStatistics, List<String> recommendations) {
    public PersonalStatistics {
        errorStatistics = List.copyOf(errorStatistics);
        recommendations = List.copyOf(recommendations);
    }
}
