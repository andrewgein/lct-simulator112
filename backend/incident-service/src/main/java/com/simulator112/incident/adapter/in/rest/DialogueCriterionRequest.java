package com.simulator112.incident.adapter.in.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DialogueCriterionRequest(
        UUID id,
        @NotBlank String name,
        @NotBlank String hypothesis,
        @Min(1) @Max(40) int weight) {
}
