package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.domain.model.Review;
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

class ReviewRestControllerTests {
    private final GetReviewUseCase getReview = mock(GetReviewUseCase.class);
    private final ReviewRestController controller = new ReviewRestController(getReview);

    @Test
    void supervisorCanReadStudentReviews() {
        UUID studentId = UUID.randomUUID();
        when(getReview.getByUserId(studentId)).thenReturn(List.of(review(studentId)));

        var response = controller.getStudentReviews("SUPERVISOR", studentId);

        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().getFirst().userId()).isEqualTo(studentId);
    }

    @Test
    void studentCannotReadAnotherStudentsReviews() {
        assertThatThrownBy(() -> controller.getStudentReviews("STUDENT", UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void supervisorCanOpenAnotherStudentsReview() {
        UUID studentId = UUID.randomUUID();
        Review review = review(studentId);
        when(getReview.getByContextId(review.contextId())).thenReturn(review);

        var response = controller.getReview(UUID.randomUUID(), "SUPERVISOR", review.contextId());

        assertThat(response.userId()).isEqualTo(studentId);
    }

    private Review review(UUID userId) {
        return new Review(UUID.randomUUID(), userId, UUID.randomUUID(), ReviewStatus.DONE,
                List.of(), Instant.now(), Instant.now());
    }
}
