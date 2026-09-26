package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.ReviewAnalytics;

public interface ReviewAnalyticsStore {
    ReviewAnalytics analyze(ReviewAnalytics.Query query, int page, int size);
}
