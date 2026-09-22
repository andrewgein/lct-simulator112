package com.simulator112.course.domain.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Course(
        UUID id,
        String title,
        String description,
        UUID authorId,
        List<CourseMaterial> materials,
        List<Assignment> assignments) {
    public Course {
        materials = materials == null ? List.of() : List.copyOf(materials);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    public Optional<Assignment> assignment(UUID assignmentId) {
        return assignments.stream().filter(assignment -> assignment.id().equals(assignmentId)).findFirst();
    }

    public Optional<Assignment> nextAssignment(List<UUID> completedAssignmentIds) {
        return assignments.stream().filter(assignment -> !completedAssignmentIds.contains(assignment.id())).findFirst();
    }
}
