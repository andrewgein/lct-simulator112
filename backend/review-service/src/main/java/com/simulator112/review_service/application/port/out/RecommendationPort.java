package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.ErrorStatistic;

import java.util.List;

public interface RecommendationPort {
    List<String> recommend(List<ErrorStatistic> statistics);
}
