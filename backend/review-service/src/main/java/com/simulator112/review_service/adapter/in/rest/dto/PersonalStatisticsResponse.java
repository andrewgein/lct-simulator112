package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;

public record PersonalStatisticsResponse(List<ErrorStatisticResponse> errorStatistics, List<String> recommendations) {
    public PersonalStatisticsResponse {
        errorStatistics = List.copyOf(errorStatistics);
        recommendations = List.copyOf(recommendations);
    }
}
