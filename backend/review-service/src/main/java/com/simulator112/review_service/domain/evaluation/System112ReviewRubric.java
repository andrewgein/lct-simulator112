package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.*;

public final class System112ReviewRubric implements ReviewRubric {
    private final Rubric rubric = new RubricBuilder(ReviewSubmission.TargetType.SYSTEM_112)
            .addStage(new Stage("Работа с карточками системы 112", List.of(
                    criterion("Поля", 70), criterion("Операции и связи", 20),
                    criterion("Обработка звонков", 10))))
            .build();

    static Map<String, ReviewSubmission.CardRevision> assemble(List<ReviewSubmission.CardRevision> revisions) {
        Map<String, List<ReviewSubmission.CardRevision>> grouped = new LinkedHashMap<>();
        revisions.forEach(value -> grouped.computeIfAbsent(value.cardId(), ignored -> new ArrayList<>()).add(value));
        Map<String, ReviewSubmission.CardRevision> result = new LinkedHashMap<>();
        grouped.forEach((cardId, history) -> {
            history.sort(Comparator.comparingLong(ReviewSubmission.CardRevision::version));
            ReviewSubmission.CardRevision state = null;
            long version = 0;
            for (var revision : history) {
                if (revision.version() <= version)
                    throw new IllegalArgumentException("Версии карточки должны возрастать: " + cardId);
                if (state != null && !state.callId().equals(revision.callId()))
                    throw new IllegalArgumentException("Карточка сменила звонок: " + cardId);
                state = merge(state, revision);
                version = revision.version();
            }
            result.put(cardId, state);
        });
        return result;
    }

    private static ReviewSubmission.CardRevision merge(ReviewSubmission.CardRevision previous,
                                                       ReviewSubmission.CardRevision current) {
        if (previous == null) return current;
        return new ReviewSubmission.CardRevision(current.revisionId(), current.cardId(), current.version(), current.callId(),
                current.parentCardId(), current.duplicateOfCardId(), current.status(),
                merge(previous.applicant(), current.applicant()), merge(previous.victim(), current.victim()),
                current.additionalInfoProvided() ? current.additionalInfo() : previous.additionalInfo(),
                current.additionalInfoProvided(), blank(current.incidentType()) ? previous.incidentType() : current.incidentType());
    }

    private static ReviewSubmission.Person merge(ReviewSubmission.Person previous, ReviewSubmission.Person current) {
        if (current == null) return previous;
        if (previous == null) return current;
        return new ReviewSubmission.Person(inherit(current.firstName(), previous.firstName()), inherit(current.lastName(), previous.lastName()),
                inherit(current.middleName(), previous.middleName()), inherit(current.phone(), previous.phone()),
                inherit(current.contactPhone(), previous.contactPhone()), inherit(current.address(), previous.address()),
                inherit(current.additionalInfo(), previous.additionalInfo()));
    }

    private static boolean same(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String inherit(String value, String previous) {
        return value == null || value.isBlank() ? previous : value;
    }

    @Override
    public boolean supports(ReviewSubmission submission) {
        return rubric.supports(submission);
    }

    @Override
    public List<CriterionResult> evaluate(ReviewSubmission submission) {
        return rubric.evaluate(submission);
    }

    public List<Stage> stages() {
        return rubric.stages();
    }

    private Criterion criterion(String category, int maxScore) {
        return new Criterion(category, maxScore) {
            @Override
            public List<CriterionResult> evaluate(ReviewSubmission submission) {
                Map<String, ReviewSubmission.CardRevision> cards = assemble(submission.cardRevisions());
                Map<String, List<ReviewSubmission.CardRevision>> byCall = new HashMap<>();
                cards.values().forEach(card -> byCall.computeIfAbsent(card.callId(), ignored -> new ArrayList<>()).add(card));
                return submission.incidents().stream().map(incident -> {
                    List<ExpectedCall> calls = expectedCalls(incident);
                    double ratio = switch (category) {
                        case "Поля" -> fieldRatio(calls, byCall);
                        case "Операции и связи" -> operationRatio(calls, byCall, cards);
                        case "Обработка звонков" -> coverageRatio(calls, byCall);
                        default -> throw new IllegalStateException("Неизвестный критерий: " + category);
                    };
                    int score = (int) Math.round(maxScore() * ratio);
                    return result(incident, score, score == maxScore()
                            ? "Критерий выполнен" : "Набрано " + score + " из " + maxScore() + " баллов");
                }).toList();
            }
        };
    }

    private double fieldRatio(List<ExpectedCall> calls, Map<String, List<ReviewSubmission.CardRevision>> byCall) {
        int checks = 0;
        int correct = 0;
        for (ExpectedCall expected : calls) {
            var cards = byCall.getOrDefault(expected.call().id(), List.of());
            var card = cards.isEmpty() ? null : cards.getFirst();
            List<Boolean> values = new ArrayList<>();
            if (expected.call().person() != null)
                comparePerson(values, expected.call().person(), card == null ? null : card.applicant());
            if (expected.stage().victim() != null)
                comparePerson(values, expected.stage().victim(), card == null ? null : card.victim());
            if (!blank(expected.stage().classifierCode()))
                values.add(card != null && same(expected.stage().classifierCode(), card.incidentType()));
            if (values.isEmpty()) values.add(card != null);
            checks += values.size();
            correct += (int) values.stream().filter(Boolean::booleanValue).count();
        }
        return checks == 0 ? 0 : (double) correct / checks;
    }

    private double operationRatio(List<ExpectedCall> calls, Map<String, List<ReviewSubmission.CardRevision>> byCall,
                                  Map<String, ReviewSubmission.CardRevision> cards) {
        if (calls.isEmpty()) return 0;
        int correct = 0;
        for (ExpectedCall expected : calls) {
            var matches = byCall.getOrDefault(expected.call().id(), List.of());
            if (matches.size() != 1) continue;
            var card = matches.getFirst();
            boolean valid = switch (expected.operation()) {
                case CREATE ->
                        "ACTIVE".equals(card.status()) && blank(card.parentCardId()) && blank(card.duplicateOfCardId());
                case CREATE_CHILD ->
                        "ACTIVE".equals(card.status()) && linked(card.parentCardId(), expected.targetCallId(), cards);
                case DUPLICATE ->
                        "CLOSED_DUPLICATE".equals(card.status()) && linked(card.duplicateOfCardId(), expected.targetCallId(), cards);
            };
            if (valid) correct++;
        }
        return (double) correct / calls.size();
    }

    private boolean linked(String targetCardId, String expectedCallId, Map<String, ReviewSubmission.CardRevision> cards) {
        var target = cards.get(targetCardId);
        return target != null && expectedCallId.equals(target.callId()) && "ACTIVE".equals(target.status());
    }

    private double coverageRatio(List<ExpectedCall> calls, Map<String, List<ReviewSubmission.CardRevision>> byCall) {
        if (calls.isEmpty()) return 0;
        return (double) calls.stream().filter(call -> byCall.getOrDefault(call.call().id(), List.of()).size() == 1).count() / calls.size();
    }

    private List<ExpectedCall> expectedCalls(ReviewSubmission.IncidentScenario incident) {
        List<ExpectedCall> result = new ArrayList<>();
        String previousCanonical = null;
        for (var stage : incident.stages().stream().sorted(Comparator.comparing(
                value -> value.position() == null ? Integer.MAX_VALUE : value.position())).toList()) {
            var calls = stage.calls().stream().sorted(Comparator.comparingInt(ReviewSubmission.CallScenario::position)).toList();
            if (calls.isEmpty()) continue;
            String canonical = calls.getFirst().id();
            result.add(new ExpectedCall(calls.getFirst(), stage, previousCanonical == null ? Operation.CREATE : Operation.CREATE_CHILD,
                    previousCanonical));
            for (int index = 1; index < calls.size(); index++) {
                result.add(new ExpectedCall(calls.get(index), stage, Operation.DUPLICATE, canonical));
            }
            previousCanonical = canonical;
        }
        return result;
    }

    private void comparePerson(List<Boolean> checks, ReviewSubmission.Person expected, ReviewSubmission.Person actual) {
        compare(checks, expected.firstName(), actual == null ? null : actual.firstName());
        compare(checks, expected.lastName(), actual == null ? null : actual.lastName());
        compare(checks, expected.middleName(), actual == null ? null : actual.middleName());
        compare(checks, expected.phone(), actual == null ? null : actual.phone());
        compare(checks, expected.contactPhone(), actual == null ? null : actual.contactPhone());
        compare(checks, expected.address(), actual == null ? null : actual.address());
        compare(checks, expected.additionalInfo(), actual == null ? null : actual.additionalInfo());
    }

    private void compare(List<Boolean> checks, String expected, String actual) {
        if (!blank(expected)) checks.add(same(expected, actual));
    }

    private enum Operation {CREATE, CREATE_CHILD, DUPLICATE}

    private record ExpectedCall(ReviewSubmission.CallScenario call, ReviewSubmission.StageScenario stage,
                                Operation operation, String targetCallId) {
    }
}
