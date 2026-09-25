package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.AddReviewCommentRequest;
import com.simulator112.review_service.application.port.in.AddReviewCommentUseCase;
import com.simulator112.review_service.application.port.in.GetReviewCommentsUseCase;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewComment;
import com.simulator112.review_service.domain.model.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewCommentRestControllerTests {
    private final GetReviewUseCase getReview = mock(GetReviewUseCase.class);
    private final GetReviewCommentsUseCase getComments = mock(GetReviewCommentsUseCase.class);
    private final AddReviewCommentUseCase addComment = mock(AddReviewCommentUseCase.class);
    private final ReviewCommentRestController controller = new ReviewCommentRestController(
            getReview, getComments, addComment);

    @Test
    void supervisorCanAddComment() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ReviewComment comment = new ReviewComment(UUID.randomUUID(), contextId, authorId,
                "Комментарий", Instant.now());
        when(addComment.add(contextId, authorId, "SUPERVISOR", "Комментарий")).thenReturn(comment);

        var response = controller.addComment(authorId, "SUPERVISOR", contextId,
                new AddReviewCommentRequest("Комментарий"));

        assertThat(response.id()).isEqualTo(comment.id());
        assertThat(response.authorId()).isEqualTo(authorId);
    }

    @Test
    void adminCanAddComment() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ReviewComment comment = new ReviewComment(UUID.randomUUID(), contextId, authorId,
                "Комментарий", Instant.now());
        when(addComment.add(contextId, authorId, "ADMIN", "Комментарий")).thenReturn(comment);

        var response = controller.addComment(authorId, "ADMIN", contextId,
                new AddReviewCommentRequest("Комментарий"));

        assertThat(response.id()).isEqualTo(comment.id());
        assertThat(response.authorId()).isEqualTo(authorId);
    }

    @Test
    void studentCannotAddComment() {
        assertThatThrownBy(() -> controller.addComment(UUID.randomUUID(), "STUDENT", UUID.randomUUID(),
                new AddReviewCommentRequest("Комментарий")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void studentCanReadCommentsForOwnReview() {
        UUID contextId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ReviewComment comment = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(),
                "Комментарий", Instant.now());
        when(getReview.getByContextId(contextId)).thenReturn(review(contextId, studentId));
        when(getComments.getByContextId(contextId)).thenReturn(List.of(comment));

        var response = controller.getComments(studentId, "STUDENT", contextId);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().text()).isEqualTo("Комментарий");
    }

    @Test
    void studentCannotReadCommentsForAnotherReview() {
        UUID contextId = UUID.randomUUID();
        when(getReview.getByContextId(contextId)).thenReturn(review(contextId, UUID.randomUUID()));

        assertThatThrownBy(() -> controller.getComments(UUID.randomUUID(), "STUDENT", contextId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private Review review(UUID contextId, UUID userId) {
        return new Review(contextId, userId, UUID.randomUUID(), ReviewStatus.DONE, List.of(),
                0, 0, 0, 0, 30, 0, null, null, null, Instant.now(), Instant.now());
    }
}
