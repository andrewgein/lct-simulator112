package com.simulator112.review_service.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseAssignmentsPort {
    Optional<CourseAssignments> findCourseForAssignment(UUID assignmentId, UUID userId);

    record CourseAssignments(UUID courseId, String courseTitle, List<UUID> assignmentIds) {
        public CourseAssignments {
            assignmentIds = List.copyOf(assignmentIds);
        }
    }
}
