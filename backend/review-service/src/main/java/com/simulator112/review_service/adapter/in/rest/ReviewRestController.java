package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.adapter.in.rest.dto.UserReviewsResponse;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewRestController {
    private final GetReviewUseCase getReview;

    @GetMapping
    public UserReviewsResponse getUserReviews(@RequestHeader("X-User-Id") UUID userId) {
        return new UserReviewsResponse(getReview.getByUserId(userId).stream().map(ReviewRestMapper::toResponse).toList());
    }

    @GetMapping("/{contextId}")
    public ReviewResponse getReview(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID contextId) {
        var review = getReview.getByContextId(contextId);
        if (!userId.equals(review.userId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Результат недоступен пользователю");
        }
        return ReviewRestMapper.toResponse(review);
    }
}
