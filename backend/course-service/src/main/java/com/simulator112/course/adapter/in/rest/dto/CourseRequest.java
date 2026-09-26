package com.simulator112.course.adapter.in.rest.dto;

import com.simulator112.course.domain.course.CourseTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CourseRequest(
        @NotBlank String title,
        String description,
        @NotNull CourseTargetType targetType,
        String ddsService,
        @NotNull List<@Valid CourseMaterialRequest> materials,
        @NotEmpty List<@Valid AssignmentRequest> assignments) {
}
