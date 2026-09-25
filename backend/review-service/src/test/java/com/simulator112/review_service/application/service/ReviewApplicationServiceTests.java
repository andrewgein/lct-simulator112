package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.port.out.DialogueAnalysisPort;
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
    private final DialogueAnalysisPort dialogueAnalysisPort = mock(DialogueAnalysisPort.class);
    private final ReviewApplicationService service = new ReviewApplicationService(store,
            List.of(new System112ReviewRubric()), dialogueAnalysisPort);

    @Test
    void persistsSubmissionIdentifiers() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID contextId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        var submission = new ReviewSubmission(contextId, userId, assignmentId,
                ReviewSubmission.TargetType.SYSTEM_112, List.of(), List.of(), List.of(),
                List.of(), null, null);

        Review result = service.submit(submission);

        assertThat(result.contextId()).isEqualTo(contextId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.assignmentId()).isEqualTo(assignmentId);
        assertThat(result.status()).isEqualTo(ReviewStatus.DONE);
        assertThat(result.finalScore()).isEqualTo(result.automaticScore());
    }

    @Test
    void addsNliDialogueScoresToScaledSystem112Rubric() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var criterion = new ReviewSubmission.DialogueCriterion(
                "address", "Уточнение адреса", "Оператор уточнил адрес происшествия", 25);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(),
                new ReviewSubmission.EvaluationCriteria(List.of(criterion)));
        var submission = new ReviewSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(), List.of(),
                List.of(new ReviewSubmission.TranscriptPhrase("USER", "Назовите адрес")), null, null);
        when(dialogueAnalysisPort.analyze(any(), any())).thenReturn(List.of(
                new DialogueAnalysisPort.DialogueAnalysis("address", true, 0.91)));

        Review result = service.submit(submission);

        assertThat(result.maxScore()).isEqualTo(100);
        assertThat(result.results()).anySatisfy(value -> {
            assertThat(value.criterionName()).isEqualTo("Уточнение адреса");
            assertThat(value.score()).isEqualTo(25);
            assertThat(value.feedback()).contains("0.910");
        });
    }

    @Test
    void expertCanCorrectCompletedAutomaticReview() {
        UUID contextId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        Review pending = review(contextId, ReviewStatus.DONE);
        when(store.findByContextId(contextId)).thenReturn(Optional.of(pending));
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Review result = service.confirm(contextId, expertId, 0, "Подтверждено");

        assertThat(result.status()).isEqualTo(ReviewStatus.DONE);
        assertThat(result.expertId()).isEqualTo(expertId);
        assertThat(result.finalScore()).isZero();
    }

    @Test
    void returnsStoredReviewByContext() {
        UUID contextId = UUID.randomUUID();
        Review review = review(contextId, ReviewStatus.IN_REVIEW);
        when(store.findByContextId(contextId)).thenReturn(Optional.of(review));

        assertThat(service.getByContextId(contextId).status()).isEqualTo(ReviewStatus.IN_REVIEW);
    }

    private Review review(UUID contextId, ReviewStatus status) {
        return new Review(contextId, UUID.randomUUID(), UUID.randomUUID(), status, List.of(),
                0, 0, 0, 0, 30, 0, null, null, null, Instant.now(), Instant.now());
    }
}
