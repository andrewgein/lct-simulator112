package com.simulator112.course.adapter.in.rest.dto;

import com.simulator112.course.domain.course.CourseTargetType;
import java.util.List;
import java.util.UUID;

public record CourseView(
        UUID id,
        String title,
        String description,
        CourseTargetType targetType,
        UUID authorId,
        List<CourseMaterialView> materials,
        List<AssignmentView> assignments) {
}
