package com.simulator112.review_service.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddReviewCommentRequest(
        @NotBlank(message = "Комментарий не может быть пустым")
        @Size(max = 4000, message = "Комментарий не может быть длиннее 4000 символов")
        String text) {
}
