package com.simulator112.course.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CourseMaterialRequest(
        UUID id,
        @NotBlank String title,
        @NotBlank String contentMarkdown) {
}
