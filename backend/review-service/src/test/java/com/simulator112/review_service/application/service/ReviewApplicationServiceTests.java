package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.evaluation.System112ReviewRubric;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewApplicationServiceTests {
    private final ReviewStore store = mock(ReviewStore.class);
    private final ReviewApplicationService service = new ReviewApplicationService(store,
            List.of(new System112ReviewRubric()));

    @Test
    void persistsSubmissionIdentifiers() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID contextId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        var submission = new ReviewSubmission(contextId, userId, levelId,
                ReviewSubmission.TargetType.SYSTEM_112, List.of(), List.of(), List.of());

        Review result = service.submit(submission);

        assertThat(result.contextId()).isEqualTo(contextId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.levelId()).isEqualTo(levelId);
        assertThat(result.status()).isEqualTo(ReviewStatus.IN_REVIEW);
    }

    @Test
    void marksReviewDoneWhenRequested() {
        UUID contextId = UUID.randomUUID();
        Review review = new Review(contextId, null, UUID.randomUUID(), ReviewStatus.IN_REVIEW,
                List.of(), Instant.now(), Instant.now());
        when(store.findByContextId(contextId)).thenReturn(Optional.of(review));
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.getByContextId(contextId).status()).isEqualTo(ReviewStatus.DONE);
    }
}
