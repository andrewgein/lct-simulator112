package com.simulator112.course.adapter.in.rest.dto;

import com.simulator112.course.domain.enrollment.EnrollmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrollmentView(
        UUID id,
        UUID courseId,
        UUID studentId,
        UUID groupId,
        EnrollmentStatus status,
        Instant materialsCompletedAt,
        List<UUID> completedAssignmentIds,
        AssignmentView currentAssignment,
        Instant completedAt) {
}
