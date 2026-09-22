package com.simulator112.course.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record CourseView(
        UUID id,
        String title,
        String description,
        UUID authorId,
        List<CourseMaterialView> materials,
        List<AssignmentView> assignments) {
}
