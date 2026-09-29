package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.ReviewSubmission;
import com.simulator112.review_service.domain.model.IncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DdsReviewRubricTests {
    private final Instant deadline = Instant.parse("2026-01-01T12:01:00Z");

    @Test
    void evaluatesTimelyStatusAndCallOnlyInReview() {
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "WAIT_FOR_BRIGADE_STATUS_CHANGE", "SUCCEEDED",
                        deadline.minusSeconds(60), deadline, null, true, List.of("call"))), List.of(
                new ReviewSubmission.ReactionEvent(IncidentStatus.ARRIVED, deadline.plusSeconds(5), "Доложили")));
        var results = evaluate(runtime);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isEqualTo(100);
        assertThat(results.stream().mapToInt(value -> value.maxScore()).sum()).isEqualTo(100);
    }

    @Test
    void lateStatusAndMissedCallLosePointsWithoutChangingTimelineOutcome() {
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "WAIT_FOR_BRIGADE_STATUS_CHANGE", "SUCCEEDED",
                        deadline.minusSeconds(60), deadline, null, false, List.of())), List.of(
                new ReviewSubmission.ReactionEvent(IncidentStatus.ARRIVED, deadline.plusSeconds(61), "Опоздал")));
        var results = evaluate(runtime);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isZero();
        assertThat(results).anySatisfy(result -> assertThat(result.feedback()).contains("пропущен"));
    }

    @Test
    void statusEnteredBeforeActualChangeDoesNotCount() {
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "WAIT_FOR_BRIGADE_STATUS_CHANGE", "SUCCEEDED",
                        deadline.minusSeconds(60), deadline, null, true, List.of("call"))), List.of(
                new ReviewSubmission.ReactionEvent(IncidentStatus.ARRIVED, deadline.minusSeconds(5), "Угадал")));

        assertThat(evaluate(runtime).stream().mapToInt(value -> value.score()).sum()).isEqualTo(50);
    }

    @Test
    void statusTriggerBeforeTimeLimitCountsAtNextStageStart() {
        var transition = deadline.minusSeconds(20);
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "WAIT_FOR_BRIGADE_STATUS_CHANGE", "SUCCEEDED",
                        deadline.minusSeconds(60), deadline, null, true, List.of("call")),
                new ReviewSubmission.StageRuntime("two", "COMPLETE_INCIDENT", "ACTIVE",
                        transition, null, null, false, List.of())), List.of(
                new ReviewSubmission.ReactionEvent(IncidentStatus.ARRIVED, transition, "Доложили")));

        assertThat(evaluate(runtime).stream().mapToInt(value -> value.score()).sum()).isEqualTo(100);
    }

    @Test
    void checksEachConfiguredCallSeparately() {
        var stage = new ReviewSubmission.StageScenario("one", null, List.of(), 0,
                "CALL_BRIGADE_FOR_STATUS", List.of(
                new ReviewSubmission.CallScenario("incoming", 0, null),
                new ReviewSubmission.CallScenario("outgoing", 1, null)), null, null);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(stage),
                new ReviewSubmission.EvaluationCriteria(List.of()));
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "CALL_BRIGADE_FOR_STATUS", "SUCCEEDED",
                        deadline.minusSeconds(60), deadline, null, true, List.of("incoming"))));
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.DDS, List.of(incident), List.of(), List.of(runtime), List.of(), null, null);

        var results = new DdsReviewRubric().evaluate(submission);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isEqualTo(50);
        assertThat(results).anySatisfy(value -> assertThat(value.feedback()).contains("outgoing", "пропущен"));
    }

    private List<com.simulator112.review_service.domain.model.CriterionResult> evaluate(ReviewSubmission.IncidentRuntime runtime) {
        var stage = new ReviewSubmission.StageScenario("one", null, List.of(), 0,
                "WAIT_FOR_BRIGADE_STATUS_CHANGE", List.of(new ReviewSubmission.CallScenario("call", 0, null)),
                null, IncidentStatus.ARRIVED);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(stage),
                new ReviewSubmission.EvaluationCriteria(List.of()));
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.DDS, List.of(incident), List.of(), List.of(runtime),
                List.of(), null, deadline.plusSeconds(60));
        return new DdsReviewRubric().evaluate(submission);
    }
}
