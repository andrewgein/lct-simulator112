package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.ReviewAnalytics;

import java.time.LocalDate;
import java.util.UUID;

public interface GetReviewAnalyticsUseCase {
    ReviewAnalytics getAnalytics(UUID requesterId, String role, Filter filter, int page, int size);

    record Filter(UUID groupId, UUID courseId, UUID assignmentId, String incidentId,
                  String difficulty, String criterion, LocalDate from, LocalDate to) {
    }
}
