package com.simulator112.course.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StudyGroupRequest(
        @NotBlank String title,
        @NotNull List<@NotNull UUID> studentIds) {
}
