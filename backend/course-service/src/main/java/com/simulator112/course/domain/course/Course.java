package com.simulator112.course.domain.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Course(
        UUID id,
        String title,
        String description,
        CourseTargetType targetType,
        DdsService ddsService,
        UUID authorId,
        List<CourseMaterial> materials,
        List<Assignment> assignments) {
    public Course {
        materials = materials == null ? List.of() : List.copyOf(materials);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    public Course(UUID id, String title, String description, CourseTargetType targetType, UUID authorId,
            List<CourseMaterial> materials, List<Assignment> assignments) {
        this(id, title, description, targetType, null, authorId, materials, assignments);
    }

    public Optional<Assignment> assignment(UUID assignmentId) {
        return assignments.stream().filter(assignment -> assignment.id().equals(assignmentId)).findFirst();
    }

}
