package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.port.in.GetReviewAnalyticsUseCase;
import com.simulator112.review_service.application.port.out.AnalyticsCatalogPort;
import com.simulator112.review_service.application.port.out.ReviewAnalyticsStore;
import com.simulator112.review_service.domain.model.ReviewAnalytics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewAnalyticsApplicationService implements GetReviewAnalyticsUseCase {
    private static final Set<String> ROLES = Set.of("ADMIN", "SUPERVISOR");
    private static final int MAX_PAGE_SIZE = 100;

    private final AnalyticsCatalogPort catalogPort;
    private final ReviewAnalyticsStore analyticsStore;

    @Override
    @Transactional(readOnly = true)
    public ReviewAnalytics getAnalytics(java.util.UUID requesterId, String role, Filter filter, int page, int size) {
        if (!ROLES.contains(role)) throw new IllegalArgumentException("Недостаточно прав для просмотра аналитики");
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        var catalog = catalogPort.getCatalog(requesterId, role);
        List<ReviewAnalytics.AccessScope> scopes = catalog.groups().stream()
                .filter(group -> filter.groupId() == null || group.id().equals(filter.groupId()))
                .map(group -> new ReviewAnalytics.AccessScope(group.id(), group.studentIds(), group.courses().stream()
                        .filter(course -> filter.courseId() == null || course.id().equals(filter.courseId()))
                        .flatMap(course -> course.assignments().stream())
                        .filter(assignment -> filter.assignmentId() == null || assignment.id().equals(filter.assignmentId()))
                        .filter(assignment -> filter.difficulty() == null || filter.difficulty().isBlank()
                                || filter.difficulty().equals(assignment.difficulty()))
                        .filter(assignment -> filter.incidentId() == null || filter.incidentId().isBlank()
                                || assignment.incidentIds().stream().anyMatch(id -> id.toString().equals(filter.incidentId())))
                        .map(AnalyticsCatalogPort.AssignmentEntry::id).distinct().toList()))
                .filter(scope -> !scope.studentIds().isEmpty() && !scope.assignmentIds().isEmpty())
                .toList();
        var query = new ReviewAnalytics.Query(scopes, blankToNull(filter.incidentId()),
                blankToNull(filter.criterion()),
                filter.from() == null ? null : filter.from().atStartOfDay().toInstant(ZoneOffset.UTC),
                filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        return analyticsStore.analyze(query, safePage, safeSize);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
