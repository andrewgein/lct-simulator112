package com.simulator112.review_service.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCriterionScoreRequest(@NotNull UUID criterionResultId, @Min(0) int score) {
}
