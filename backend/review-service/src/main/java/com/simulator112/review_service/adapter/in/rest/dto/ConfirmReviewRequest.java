package com.simulator112.review_service.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;

public record ConfirmReviewRequest(@Min(0) Integer finalScore, String comment) {
}
