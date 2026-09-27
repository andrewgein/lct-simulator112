package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DdsReviewRubricTests {
    @Test
    void evaluatesStageOutcomesAndTerminalStatus() {
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(), criteria());
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "COMPLETED", List.of(
                new ReviewSubmission.StageRuntime("one", "ASSIGN_BRIGADE", "SUCCEEDED", null, null),
                new ReviewSubmission.StageRuntime("two", "COMPLETE_INCIDENT", "SUCCEEDED", null, null)));
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.DDS, List.of(incident), List.of(), List.of(runtime),
                List.of(), null, null);

        var results = new DdsReviewRubric().evaluate(submission);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isEqualTo(100);
        assertThat(results.stream().mapToInt(value -> value.maxScore()).sum()).isEqualTo(100);
    }

    @Test
    void failedStageAndIncidentReduceScore() {
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, "Инцидент", List.of(), criteria());
        var runtime = new ReviewSubmission.IncidentRuntime("incident", "FAILED", List.of(
                new ReviewSubmission.StageRuntime("one", "ASSIGN_BRIGADE", "FAILED", null, null)));
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.DDS, List.of(incident), List.of(), List.of(runtime),
                List.of(), null, null);

        var results = new DdsReviewRubric().evaluate(submission);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isZero();
        assertThat(results).anySatisfy(result -> assertThat(result.feedback())
                .contains("ASSIGN_BRIGADE", "FAILED"));
    }

    private ReviewSubmission.EvaluationCriteria criteria() {
        return new ReviewSubmission.EvaluationCriteria(List.of());
    }
}
