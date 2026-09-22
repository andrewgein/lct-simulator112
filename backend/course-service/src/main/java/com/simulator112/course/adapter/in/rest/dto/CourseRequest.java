package com.simulator112.course.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CourseRequest(
        @NotBlank String title,
        String description,
        @NotEmpty List<@Valid CourseMaterialRequest> materials,
        @NotEmpty List<@Valid AssignmentRequest> assignments) {
}
