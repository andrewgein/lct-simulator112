package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.ConfirmReviewRequest;
import com.simulator112.review_service.application.port.in.ConfirmReviewUseCase;
import com.simulator112.review_service.application.port.in.GetPersonalStatisticsUseCase;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.application.port.out.CallRecordingStore;
import com.simulator112.review_service.domain.model.CallRecording;
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
    private final ConfirmReviewUseCase confirmReview = mock(ConfirmReviewUseCase.class);
    private final CallRecordingStore callRecordings = mock(CallRecordingStore.class);
    private final GetPersonalStatisticsUseCase getStatistics = mock(GetPersonalStatisticsUseCase.class);
    private final ReviewRestController controller = new ReviewRestController(getReview, confirmReview, callRecordings, getStatistics);

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
    void supervisorCanCorrectAutomaticScore() {
        UUID contextId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        Review corrected = review(UUID.randomUUID());
        when(confirmReview.confirm(contextId, expertId, 80, "Исправлено преподавателем"))
                .thenReturn(corrected);

        var response = controller.confirmReview(expertId, "SUPERVISOR", contextId,
                new ConfirmReviewRequest(80, "Исправлено преподавателем"));

        assertThat(response.contextId()).isEqualTo(corrected.contextId());
    }

    @Test
    void studentCannotCorrectAutomaticScore() {
        assertThatThrownBy(() -> controller.confirmReview(UUID.randomUUID(), "STUDENT", UUID.randomUUID(),
                new ConfirmReviewRequest(80, null)))
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

    @Test
    void studentGetsRecordingsOnlyForRequestedOwnReview() {
        UUID studentId = UUID.randomUUID();
        Review review = review(studentId);
        var recording = new CallRecording("call-1", "20260926T120000_000000Z.wav", Instant.now());
        when(getReview.getByContextId(review.contextId())).thenReturn(review);
        when(callRecordings.findByContextId(review.contextId())).thenReturn(List.of(recording));

        var response = controller.getRecordings(studentId, "STUDENT", review.contextId());

        assertThat(response.recordings()).hasSize(1);
        assertThat(response.recordings().getFirst().callId()).isEqualTo("call-1");
    }

    @Test
    void studentCannotGetRecordingsFromAnotherReview() {
        Review review = review(UUID.randomUUID());
        when(getReview.getByContextId(review.contextId())).thenReturn(review);

        assertThatThrownBy(() -> controller.getRecordings(
                UUID.randomUUID(), "STUDENT", review.contextId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private Review review(UUID userId) {
        return new Review(UUID.randomUUID(), userId, UUID.randomUUID(), ReviewStatus.DONE, List.of(),
                0, 0, 0, 0, 30, 0, null, null, null, Instant.now(), Instant.now());
    }
}
