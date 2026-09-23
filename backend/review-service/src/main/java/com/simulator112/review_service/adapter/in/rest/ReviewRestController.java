package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.adapter.in.rest.dto.UserReviewsResponse;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewRestController {
    private static final Set<String> REVIEWER_ROLES = Set.of("ADMIN", "SUPERVISOR");
    private final GetReviewUseCase getReview;

    @GetMapping
    public UserReviewsResponse getUserReviews(@RequestHeader("X-User-Id") UUID userId) {
        return reviewsFor(userId);
    }

    @GetMapping("/users/{studentId}")
    public UserReviewsResponse getStudentReviews(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID studentId) {
        requireReviewer(role);
        return reviewsFor(studentId);
    }

    @GetMapping("/{contextId}")
    public ReviewResponse getReview(@RequestHeader("X-User-Id") UUID userId,
                                    @RequestHeader("X-User-Role") String role,
                                    @PathVariable UUID contextId) {
        var review = getReview.getByContextId(contextId);
        if (!userId.equals(review.userId()) && !REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Результат недоступен пользователю");
        }
        return ReviewRestMapper.toResponse(review);
    }

    private UserReviewsResponse reviewsFor(UUID userId) {
        return new UserReviewsResponse(getReview.getByUserId(userId).stream()
                .map(ReviewRestMapper::toResponse).toList());
    }

    private void requireReviewer(String role) {
        if (!REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Просмотр результатов доступен преподавателям");
        }
    }
}
