package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.AddReviewCommentRequest;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewCommentResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewCommentsResponse;
import com.simulator112.review_service.application.port.in.AddReviewCommentUseCase;
import com.simulator112.review_service.application.port.in.GetReviewCommentsUseCase;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.domain.model.ReviewComment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review/{contextId}/comments")
@RequiredArgsConstructor
public class ReviewCommentRestController {
    private static final Set<String> REVIEWER_ROLES = Set.of("ADMIN", "SUPERVISOR");

    private final GetReviewUseCase getReview;
    private final GetReviewCommentsUseCase getComments;
    private final AddReviewCommentUseCase addComment;

    @GetMapping
    public ReviewCommentsResponse getComments(@RequestHeader("X-User-Id") UUID userId,
                                              @RequestHeader("X-User-Role") String role,
                                              @PathVariable UUID contextId) {
        var review = getReview.getByContextId(contextId);
        if (!userId.equals(review.userId()) && !REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Комментарии недоступны пользователю");
        }
        return new ReviewCommentsResponse(getComments.getByContextId(contextId).stream()
                .map(ReviewCommentRestController::toResponse).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCommentResponse addComment(@RequestHeader("X-User-Id") UUID authorId,
                                            @RequestHeader("X-User-Role") String role,
                                            @PathVariable UUID contextId,
                                            @Valid @RequestBody AddReviewCommentRequest request) {
        if (!REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Оставлять комментарии может только преподаватель или администратор");
        }
        return toResponse(addComment.add(contextId, authorId, role, request.text()));
    }

    private static ReviewCommentResponse toResponse(ReviewComment comment) {
        return new ReviewCommentResponse(comment.id(), comment.reviewContextId(), comment.authorId(),
                comment.text(), comment.createdAt());
    }
}
