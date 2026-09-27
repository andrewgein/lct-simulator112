package com.simulator112.review_service.application.port.in;

import java.util.List;
import java.util.UUID;

public interface GetAssignmentResultsUseCase {
    List<Result> get(UUID userId, List<UUID> assignmentIds);

    record Result(UUID assignmentId, int score, int maxScore, Integer grade) {}
}
