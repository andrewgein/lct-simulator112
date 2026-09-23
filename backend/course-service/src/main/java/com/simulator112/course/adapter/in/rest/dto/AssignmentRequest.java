package com.simulator112.course.adapter.in.rest.dto;

import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignmentRequest(
        UUID id,
        @NotBlank String title,
        String description,
        @NotNull AssignmentDifficulty difficulty,
        @NotNull AssignmentExecutionMode executionMode,
        @NotEmpty List<@NotNull UUID> incidentIds) {
}
