package com.simulator112.course.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CourseMaterialRequest(
        UUID id,
        @NotBlank String title,
        @NotBlank String fileObjectKey,
        @NotBlank String fileName,
        @NotBlank String fileContentType,
        @NotNull @PositiveOrZero Long fileSize) {
}
