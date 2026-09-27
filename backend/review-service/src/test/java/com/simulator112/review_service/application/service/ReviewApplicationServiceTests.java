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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewApplicationServiceTests {
    private final ReviewStore store = mock(ReviewStore.class);
    private final DialogueAnalysisPort dialogueAnalysisPort = mock(DialogueAnalysisPort.class);
    private final org.springframework.context.ApplicationEventPublisher events = mock(org.springframework.context.ApplicationEventPublisher.class);
    private final ReviewApplicationService service = new ReviewApplicationService(store,
            List.of(new System112ReviewRubric()), dialogueAnalysisPort, events);

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
        assertThat(result.grade()).isNull();
        assertThat(result.passed()).isNull();
    }

    @Test
    void addsNliDialogueScoresToScaledSystem112Rubric() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var criterion = new ReviewSubmission.DialogueCriterion(
                "address", "Уточнение адреса", "Оператор уточнил адрес происшествия", 25);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(),
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
    void belowPassingThresholdIsNotCreditedAndCorrectionRecalculatesGrade() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID contextId = UUID.randomUUID();
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(),
                new ReviewSubmission.EvaluationCriteria(List.of()));
        var submission = new ReviewSubmission(contextId, UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(), List.of(), List.of(),
                null, null, 40, 60, 80);

        Review review = service.submit(submission);
        assertThat(review.grade()).isEqualTo(2);
        assertThat(review.passed()).isFalse();
        assertThat(review.status()).isEqualTo(ReviewStatus.DONE);

        when(store.findByContextId(contextId)).thenReturn(Optional.of(review));
        Review corrected = service.confirm(contextId, UUID.randomUUID(), 60, "Исправлено");
        assertThat(corrected.grade()).isEqualTo(4);
        assertThat(corrected.passed()).isTrue();
        assertThat(corrected.threshold3()).isEqualTo(40);
        assertThat(corrected.confirm(UUID.randomUUID(), 39, null, Instant.now()).passed()).isFalse();
    }

    @Test
    void gradesTotalScoreAcrossIncidentsWithoutPerIncidentMinimum() {
        Review review = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ReviewStatus.DONE,
                List.of(), 159, 159, 200, 0, 30, 0, null, null, null, null, null, 40, 60, 80);

        assertThat(review.grade()).isEqualTo(4);
        assertThat(review.passed()).isTrue();
        assertThat(review.confirm(UUID.randomUUID(), 160, null, Instant.now()).grade()).isEqualTo(5);
        assertThat(review.confirm(UUID.randomUUID(), 120, null, Instant.now()).grade()).isEqualTo(4);
        assertThat(review.confirm(UUID.randomUUID(), 80, null, Instant.now()).grade()).isEqualTo(3);
        assertThat(review.confirm(UUID.randomUUID(), 79, null, Instant.now()).passed()).isFalse();
    }

    @Test
    void submitCapturesIncidentAndDispatcherCardSnapshots() {
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var stage = new ReviewSubmission.StageScenario("stage", 0, List.of("fire"), 2, null,
                List.of(new ReviewSubmission.CallScenario("call", 0, null)));
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Пожар в квартире", List.of(stage),
                new ReviewSubmission.EvaluationCriteria(List.of()));
        var applicant = new ReviewSubmission.Person("Иван", "Иванов", null, "1234567890",
                null, null, "ул. Ленина, 1", null);
        var card = new ReviewSubmission.CardRevision("revision", "card", 1, "call", null, applicant, 2,
                Map.of(), false, List.of("fire"), List.of("Пожарная служба"), Instant.now());
        var submission = new ReviewSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(card), List.of(), List.of(),
                null, null);

        Review result = service.submit(submission);

        assertThat(result.incidents()).hasSize(1);
        assertThat(result.incidents().getFirst().title()).isEqualTo("Пожар в квартире");
        assertThat(result.incidents().getFirst().victimCount()).isEqualTo(2);
        assertThat(result.incidents().getFirst().classifierCodes()).containsExactly("fire");
        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().getFirst().applicant().lastName()).isEqualTo("Иванов");
        assertThat(result.cards().getFirst().services()).containsExactly("Пожарная служба");
        assertThat(result.cards().getFirst().incidentId()).isEqualTo("incident");
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
