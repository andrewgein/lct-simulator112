package com.simulator112.review_service.config.rubric;

import com.simulator112.context.grpc.contract.*;
import com.simulator112.incident.grpc.contract.*;
import com.simulator112.review_service.model.entity.CriterionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelReviewTests {
    private DialupContext call(String id) {
        return DialupContext.newBuilder().setId(id).build();
    }

    private StageContext stage(int position, DialupContext... calls) {
        return StageContext.newBuilder()
                .setPosition(position)
                .setType(IncidentTypeInfo.newBuilder().setId("fire"))
                .addAllDialups(List.of(calls))
                .build();
    }

    private IncidentContext incident(String id) {
        return incident(id, new StageContext[0]);
    }

    private IncidentContext incident(String id, DialupContext... calls) {
        return incident(id, stage(0, calls));
    }

    private IncidentContext incident(String id, StageContext... stages) {
        return IncidentContext.newBuilder().setId(id).addAllStages(List.of(stages)).build();
    }

    private SolutionContext.Builder card(String id, String call) {
        return SolutionContext.newBuilder().setCardId(id).setDialupId(call).setVersion(1)
                .setStatus(SolutionContextStatus.ACTIVE).setIncidentType("fire");
    }

    private FullContext context(List<IncidentContext> incidents, SolutionContext... cards) {
        return FullContext.newBuilder().setLevelContext(LevelContext.newBuilder().addAllIncidents(incidents))
                .addAllSolutionContextRevisions(List.of(cards)).build();
    }

    private int score(List<CriterionResult> results) {
        return results.stream().mapToInt(CriterionResult::getScore).sum();
    }

    private int categoryScore(List<CriterionResult> results, int incidentOrder, String name) {
        return results.stream().filter(result -> result.getIncidentOrder() == incidentOrder
                        && result.getCriterionName().equals(name))
                .mapToInt(CriterionResult::getScore).sum();
    }

    private int categoryMax(List<CriterionResult> results, int incidentOrder, String name) {
        return results.stream().filter(result -> result.getIncidentOrder() == incidentOrder
                        && result.getCriterionName().equals(name))
                .mapToInt(CriterionResult::getMaxScore).sum();
    }

    @Test
    void derivesCreateDuplicateAndChildFromHierarchy() {
        var one = incident("one", call("a"));
        var two = incident("two", stage(0, call("b"), call("d")), stage(1, call("c")));
        var results = LevelReview.evaluate(context(List.of(one, two), card("ca", "a").build(),
                card("cb", "b").build(),
                card("cd", "d").setDuplicateOfId("cb").setStatus(SolutionContextStatus.CLOSED_DUPLICATE).build(),
                card("cc", "c").setParentId("cb").build()));
        assertEquals(200, score(results));
        assertEquals(200, results.stream().mapToInt(CriterionResult::getMaxScore).sum());
        assertEquals(70, categoryMax(results, 1, "Поля"));
        assertEquals(20, categoryMax(results, 1, "Операции и связи"));
        assertEquals(10, categoryMax(results, 1, "Обработка звонков"));
        assertEquals(70, categoryMax(results, 2, "Поля"));
        assertEquals(20, categoryMax(results, 2, "Операции и связи"));
        assertEquals(10, categoryMax(results, 2, "Обработка звонков"));
        assertTrue(results.stream().allMatch(result -> result.getIncidentId().equals(
                result.getIncidentOrder() == 1 ? "one" : "two")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().startsWith("Звонок 1:")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().startsWith("Звонок 4:")));
    }

    @Test
    void missingCallCannotBorrowCompatibilityCard() {
        var incident = incident("one", call("a"));
        var payload = context(List.of(incident)).toBuilder().setSolutionContext(card("legacy", "a")).build();
        assertEquals(0, score(LevelReview.evaluate(payload)));
    }

    @Test
    void missingOneOfTwoCallsLosesHalfThePoints() {
        var incident = incident("one", call("a"), call("b"));
        assertEquals(50, score(LevelReview.evaluate(context(List.of(incident), card("ca", "a").build()))));
    }

    @Test
    void extraCardsLoseCoveragePointsWithoutIncreasingFieldOrOperationBudget() {
        var incident = incident("one", call("a"));
        assertEquals(90, score(LevelReview.evaluate(context(List.of(incident),
                card("ca", "a").build(), card("extra", "a").build()))));
    }

    @Test
    void childLinkMustReferenceParentCardIdFromPreviousStageCanonicalCall() {
        var incident = incident("one", stage(0, call("a")), stage(1, call("b")));
        var results = LevelReview.evaluate(context(List.of(incident), card("ca", "a").build(),
                card("cb", "b").setParentId("a").build()));
        assertEquals(90, score(results));
        assertEquals(10, categoryScore(results, 1, "Операции и связи"));
    }

    @Test
    void fieldsComeFromDialupAndStage() {
        var incident = incident("one", call("a"));
        var results = LevelReview.evaluate(context(List.of(incident), card("ca", "a").setIncidentType("medical").build()));
        assertEquals(30, score(results));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Тип инцидента указано неверно")));
    }

    @Test
    void usesFirstCardAppearanceAsDisplayCallOrderWithoutExposingUuid() {
        var incident = incident("incident-uuid", call("dialup-a"), call("dialup-b"));
        var results = LevelReview.evaluate(context(List.of(incident),
                card("cb", "dialup-b").setIncidentType("wrong").build(), card("ca", "dialup-a").build()));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback()
                .equals("Звонок 1: Тип инцидента указано неверно")));
        assertTrue(results.stream().allMatch(result -> result.getIncidentId().equals("incident-uuid")));
        assertTrue(results.stream().noneMatch(result -> result.getCriterionName().contains("incident-uuid")
                || result.getFeedback().contains("dialup-a") || result.getFeedback().contains("dialup-b")));
    }

    @Test
    void reportsApplicantAndVictimNameErrorsSeparately() {
        var call = DialupContext.newBuilder().setId("a").setApplicant(Applicant.newBuilder()
                .setFirstName("Anna").setLastName("Smith").setMiddleName("Lee")).build();
        var stage = stage(0, call).toBuilder().setVictim(Applicant.newBuilder()
                .setFirstName("John").setLastName("Brown").setMiddleName("Ray")).build();
        var results = LevelReview.evaluate(context(List.of(incident("one", stage)), card("ca", "a").build()));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Имя заявителя указано неверно")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Фамилия заявителя указано неверно")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Отчество заявителя указано неверно")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Имя пострадавшего указано неверно")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Фамилия пострадавшего указано неверно")));
        assertTrue(results.stream().anyMatch(result -> result.getFeedback().contains("Отчество пострадавшего указано неверно")));
        assertEquals(100, results.stream().mapToInt(CriterionResult::getMaxScore).sum());
    }

    @Test
    void revisionsAreSortedAndSparseFieldsInheritedWhileMapsAreReplacedOrCleared() {
        var first = card("ca", "a").setApplicant(PersonInfo.newBuilder().setFirstName("Anna").setPhone("123"))
                .putAdditionalInfo("old", "value").setAdditionalInfoProvided(true).build();
        var second = card("ca", "a").setVersion(2).setIncidentType("")
                .setApplicant(PersonInfo.newBuilder().setPhone("456"))
                .setAdditionalInfoProvided(false).build();
        var assembled = LevelReview.assemble(List.of(second, first)).get("ca");
        assertEquals("Anna", assembled.getApplicant().getFirstName());
        assertEquals("456", assembled.getApplicant().getPhone());
        assertEquals("fire", assembled.getIncidentType());
        assertEquals("value", assembled.getAdditionalInfoOrThrow("old"));
        var third = card("ca", "a").setVersion(3).setAdditionalInfoProvided(true).build();
        assertEquals(0, LevelReview.assemble(List.of(third, first, second)).get("ca").getAdditionalInfoCount());
        third = third.toBuilder().putAdditionalInfo("new", "new value").build();
        assertEquals(java.util.Map.of("new", "new value"),
                LevelReview.assemble(List.of(third, first, second)).get("ca").getAdditionalInfoMap());
    }

    @Test
    void revisionsAreNotCountedAsExtraCards() {
        var incident = incident("one", call("a"));
        assertEquals(100, score(LevelReview.evaluate(context(List.of(incident),
                card("ca", "a").setIncidentType("wrong").build(), card("ca", "a").setVersion(2).build()))));
    }

    @Test
    void rejectsUnknownDialupsAndDuplicateVersions() {
        var incident = incident("one", call("a"));
        assertThrows(IllegalArgumentException.class, () -> LevelReview.evaluate(context(List.of(incident), card("ca", "unknown").build())));
        assertThrows(IllegalArgumentException.class, () -> LevelReview.assemble(List.of(card("ca", "a").build(), card("ca", "a").build())));
    }

    @Test
    void emptyIncidentStillHas100PossiblePoints() {
        var results = LevelReview.evaluate(context(List.of(incident("empty"))));
        assertEquals(0, score(results));
        assertEquals(100, results.stream().mapToInt(CriterionResult::getMaxScore).sum());
    }

    @Test
    void legacyPayloadHas100PossiblePoints() {
        var results = new RubricConfiguration().rubric().evaluate(FullContext.getDefaultInstance());
        assertEquals(100, results.stream().mapToInt(CriterionResult::getMaxScore).sum());
    }
}
