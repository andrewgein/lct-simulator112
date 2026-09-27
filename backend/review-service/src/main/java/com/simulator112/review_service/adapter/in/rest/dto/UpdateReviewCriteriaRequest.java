package com.simulator112.review_service.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateReviewCriteriaRequest(@NotEmpty @Valid List<UpdateCriterionScoreRequest> corrections) {
}
