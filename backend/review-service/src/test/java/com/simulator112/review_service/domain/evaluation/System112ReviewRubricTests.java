package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class System112ReviewRubricTests {
    @Test
    void awardsFullScoreForCorrectCardLinksAndFields() {
        var person = new ReviewSubmission.Person("Анна", "Иванова", null, "112", null, "Москва", null);
        var first = new ReviewSubmission.CallScenario("call-1", 0, person);
        var second = new ReviewSubmission.CallScenario("call-2", 0, person);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage-1", 0, "fire", null, null, List.of(first)),
                new ReviewSubmission.StageScenario("stage-2", 1, "fire", null, null, List.of(second))));
        var card1 = card("card-1", "call-1", "", person);
        var card2 = card("card-2", "call-2", "card-1", person);
        var submission = new ReviewSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(card1, card2), List.of());

        var results = new System112ReviewRubric().evaluate(submission);

        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isEqualTo(100);
        assertThat(results.stream().mapToInt(value -> value.maxScore()).sum()).isEqualTo(100);
    }

    @Test
    void missingCardLosesCoverageFieldAndOperationPoints() {
        var call = new ReviewSubmission.CallScenario("call", 0, null);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, "fire", null, null, List.of(call))));
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(), List.of());

        assertThat(new System112ReviewRubric().evaluate(submission).stream().mapToInt(value -> value.score()).sum()).isZero();
    }

    private ReviewSubmission.CardRevision card(String id, String callId, String mainCardId,
                                               ReviewSubmission.Person person) {
        return new ReviewSubmission.CardRevision(UUID.randomUUID().toString(), id, 1, callId, mainCardId,
                person, null, Map.of(), true, "fire");
    }
}
