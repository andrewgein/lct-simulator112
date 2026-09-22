package com.simulator112.course.domain.enrollment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Enrollment(
        UUID id,
        UUID courseId,
        UUID studentId,
        UUID groupId,
        Instant materialsCompletedAt,
        List<UUID> completedAssignmentIds,
        Instant completedAt) {
    public Enrollment {
        completedAssignmentIds = completedAssignmentIds == null ? List.of() : List.copyOf(completedAssignmentIds);
    }

    public boolean materialsCompleted() {
        return materialsCompletedAt != null;
    }

    public EnrollmentStatus status() {
        if (completedAt != null) {
            return EnrollmentStatus.COMPLETED;
        }
        return materialsCompleted() ? EnrollmentStatus.ASSIGNMENTS : EnrollmentStatus.MATERIALS;
    }

    public Enrollment withMaterialsCompletedAt(Instant value) {
        return new Enrollment(id, courseId, studentId, groupId, value, completedAssignmentIds, completedAt);
    }

    public Enrollment withCompletedAssignment(UUID assignmentId, boolean lastAssignment, Instant completedNow) {
        List<UUID> completed = new java.util.ArrayList<>(completedAssignmentIds);
        completed.add(assignmentId);
        return new Enrollment(id, courseId, studentId, groupId, materialsCompletedAt, completed,
                lastAssignment ? completedNow : completedAt);
    }
}
