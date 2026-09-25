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
        var person = new ReviewSubmission.Person("Анна", "Иванова", null, "112", null, null, "Москва", null);
        var first = new ReviewSubmission.CallScenario("call-1", 0, person);
        var second = new ReviewSubmission.CallScenario("call-2", 0, person);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage-1", 0, List.of("fire"), 0, null, List.of(first)),
                new ReviewSubmission.StageScenario("stage-2", 1, List.of("fire"), 0, null, List.of(second))),
                criteria());
        var card1 = card("card-1", "call-1", "", person);
        var card2 = card("card-2", "call-2", "card-1", person);
        var submission = new ReviewSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(card1, card2), List.of(),
                List.of(), null, null);

        var results = new System112ReviewRubric().evaluate(submission);

        assertThat(results).hasSize(16);
        assertThat(results.stream().filter(value -> value.criterionName().equals("Поля"))).hasSize(12);
        assertThat(results.stream().filter(value -> value.criterionName().equals("Операции и связи"))).hasSize(2);
        assertThat(results.stream().filter(value -> value.criterionName().equals("Обработка звонков"))).hasSize(2);
        assertThat(results.stream().mapToInt(value -> value.score()).sum()).isEqualTo(100);
        assertThat(results.stream().mapToInt(value -> value.maxScore()).sum()).isEqualTo(100);
        assertThat(results).allSatisfy(result -> assertThat(result.feedback())
                .doesNotContain("call-1", "call-2"));
        assertThat(results).anySatisfy(result -> assertThat(result.feedback()).contains("Звонок №1"));
    }

    @Test
    void reservesConfiguredWeightForDialogueCriteria() {
        var person = new ReviewSubmission.Person("Анна", "Иванова", null, "112", null, null, "Москва", null);
        var call = new ReviewSubmission.CallScenario("call", 0, person);
        var criteria = new ReviewSubmission.EvaluationCriteria(List.of(
                new ReviewSubmission.DialogueCriterion("address", "Уточнение адреса",
                        "Оператор уточнил адрес происшествия", 15),
                new ReviewSubmission.DialogueCriterion("victims", "Уточнение пострадавших",
                        "Оператор уточнил наличие пострадавших", 10)));
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, List.of("fire"), 0, null, List.of(call))),
                criteria);
        var submission = new ReviewSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident),
                List.of(card("card", "call", "", person)), List.of(),
                List.of(new ReviewSubmission.TranscriptPhrase("USER", "Уточните адрес происшествия")), null, null);

        var results = new System112ReviewRubric().evaluate(submission);

        assertThat(results.stream().mapToInt(value -> value.maxScore()).sum()).isEqualTo(75);
        assertThat(results).noneMatch(value -> value.criterionName().equals("Уточнение адреса"));
    }

    @Test
    void missingCardLosesCoverageFieldAndOperationPoints() {
        var call = new ReviewSubmission.CallScenario("call", 0, null);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, List.of("fire"), 0, null, List.of(call))),
                criteria());
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(), List.of(),
                List.of(), null, null);

        assertThat(new System112ReviewRubric().evaluate(submission).stream().mapToInt(value -> value.score()).sum()).isZero();
    }

    @Test
    void acceptsIncidentTypesInAnyOrder() {
        assertThat(fieldScore(List.of("fire", "gas"), List.of("GAS", "fire"))).isEqualTo(70);
    }

    @Test
    void rejectsMissingIncidentType() {
        assertThat(fieldScore(List.of("fire", "gas"), List.of("fire"))).isEqualTo(35);
    }

    @Test
    void rejectsExtraIncidentType() {
        assertThat(fieldScore(List.of("fire", "gas"), List.of("fire", "gas", "police"))).isEqualTo(35);
    }

    @Test
    void explainsConcreteFieldMismatchInCriterionFeedback() {
        var call = new ReviewSubmission.CallScenario("call", 0, null);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, List.of("fire"), 2, null, List.of(call))),
                criteria());
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident),
                List.of(card("card", "call", "", null, List.of("fire"))), List.of(),
                List.of(), null, null);

        var fields = new System112ReviewRubric().evaluate(submission).stream()
                .filter(result -> result.criterionName().equals("Поля")).findFirst().orElseThrow();

        assertThat(fields.feedback()).contains("Количество пострадавших указано неверно")
                .doesNotContain("ожидалось", "«2»");
    }

    @Test
    void feedbackDoesNotRevealExpectedValuesOrRequiredQuestions() {
        var expected = new ReviewSubmission.Person("СекретноеИмя", null, null, null,
                null, null, null, null);
        var actual = new ReviewSubmission.Person("ДругоеИмя", null, null, null,
                null, null, null, null);
        var call = new ReviewSubmission.CallScenario("call", 0, expected);
        var criteria = new ReviewSubmission.EvaluationCriteria(List.of(
                new ReviewSubmission.DialogueCriterion("secret", "Проверка вопроса",
                        "Назовите секретный код", 10)));
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, List.of("fire"), 0, null, List.of(call))),
                criteria);
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident),
                List.of(card("card", "call", "", actual)), List.of(), List.of(), null, null);

        String feedback = new System112ReviewRubric().evaluate(submission).stream()
                .map(result -> result.feedback()).reduce("", (left, right) -> left + " " + right);

        assertThat(feedback).contains("Имя указано неверно")
                .doesNotContain("СекретноеИмя", "Назовите секретный код");
    }

    private int fieldScore(List<String> expectedTypes, List<String> cardTypes) {
        var call = new ReviewSubmission.CallScenario("call", 0, null);
        var incident = new ReviewSubmission.IncidentScenario("incident", 1, List.of(
                new ReviewSubmission.StageScenario("stage", 0, expectedTypes, 0, null, List.of(call))),
                criteria());
        var card = card("card", "call", "", null, cardTypes);
        var submission = new ReviewSubmission(UUID.randomUUID(), null, UUID.randomUUID(),
                ReviewSubmission.TargetType.SYSTEM_112, List.of(incident), List.of(card), List.of(),
                List.of(), null, null);
        return new System112ReviewRubric().evaluate(submission).stream()
                .filter(value -> value.criterionName().equals("Поля"))
                .mapToInt(value -> value.score()).sum();
    }

    private ReviewSubmission.CardRevision card(String id, String callId, String mainCardId,
                                               ReviewSubmission.Person person) {
        return card(id, callId, mainCardId, person, List.of("fire"));
    }

    private ReviewSubmission.CardRevision card(String id, String callId, String mainCardId,
                                               ReviewSubmission.Person person, List<String> incidentTypes) {
        return new ReviewSubmission.CardRevision(UUID.randomUUID().toString(), id, 1, callId, mainCardId,
                person, 0, Map.of(), true, incidentTypes, List.of(), null);
    }

    private ReviewSubmission.EvaluationCriteria criteria() {
        return new ReviewSubmission.EvaluationCriteria(List.of());
    }
}
