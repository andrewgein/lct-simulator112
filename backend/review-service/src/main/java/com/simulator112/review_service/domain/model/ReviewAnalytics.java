package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReviewAnalytics(Summary summary, List<TrendPoint> trend, List<CriterionStat> criteria,
                              List<StudentCriterionStat> heatmap, List<ScenarioStat> scenarios, Page reviews) {
    public ReviewAnalytics {
        trend = List.copyOf(trend);
        criteria = List.copyOf(criteria);
        heatmap = List.copyOf(heatmap);
        scenarios = List.copyOf(scenarios);
    }

    public record Summary(long attempts, long students, Integer averageScore, Long averageDurationSeconds,
                          long overtimeAttempts) {
    }

    public record TrendPoint(LocalDate date, int averageScore) {
    }

    public record CriterionStat(String name, long assessments, long score, long maxScore) {
    }

    public record StudentCriterionStat(UUID userId, String criterion, long score, long maxScore) {
    }

    public record ScenarioStat(String incidentId, int incidentOrder, long attempts, long score, long maxScore) {
    }

    public record Page(List<Review> items, int page, int size, long totalItems, int totalPages) {
        public Page {
            items = List.copyOf(items);
        }
    }

    public record Query(List<AccessScope> accessScopes, String incidentId, String criterion,
                        Instant from, Instant to) {
        public Query {
            accessScopes = List.copyOf(accessScopes);
        }
    }

    public record AccessScope(UUID groupId, List<UUID> studentIds, List<UUID> assignmentIds) {
        public AccessScope {
            studentIds = List.copyOf(studentIds);
            assignmentIds = List.copyOf(assignmentIds);
        }
    }
}
