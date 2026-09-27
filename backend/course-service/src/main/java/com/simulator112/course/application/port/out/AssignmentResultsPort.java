package com.simulator112.course.application.port.out;

import java.util.List;
import java.util.UUID;

public interface AssignmentResultsPort {
    List<Result> get(UUID userId, List<UUID> assignmentIds);

    record Result(UUID assignmentId, int score, int maxScore, Integer grade) {}
}
