package com.simulator112.review_service.config.rubric;

import com.simulator112.context.grpc.contract.*;
import com.simulator112.incident.grpc.contract.*;
import com.simulator112.review_service.model.entity.CriterionResult;

import java.util.*;

public final class LevelReview {
    private static final int FIELD_POINTS = 70;
    private static final int OPERATION_POINTS = 20;
    private static final int COVERAGE_POINTS = 10;

    private LevelReview() {}

    public static List<CriterionResult> evaluate(FullContext context) {
        List<IncidentContext> incidents = context.hasLevelContext()
                ? context.getLevelContext().getIncidentsList() : List.of(context.getIncidentContext());
        Map<String, SolutionContext> cards = assemble(context.getSolutionContextRevisionsList());
        Map<String, List<SolutionContext>> byDialup = new HashMap<>();
        cards.values().forEach(card -> byDialup.computeIfAbsent(card.getDialupId(), ignored -> new ArrayList<>()).add(card));
        validateDialups(incidents, byDialup.keySet());
        Map<String, Integer> callNumbers = callNumbers(incidents, context.getSolutionContextRevisionsList());

        List<CriterionResult> results = new ArrayList<>();
        for (int incidentIndex = 0; incidentIndex < incidents.size(); incidentIndex++) {
            IncidentContext incident = incidents.get(incidentIndex);
            IncidentRef incidentRef = new IncidentRef(incident.getId(), incidentIndex + 1);
            List<ExpectedCall> calls = expectedCalls(incident);
            if (calls.isEmpty()) {
                results.add(singleResult(incidentRef, "Поля", FIELD_POINTS, false,
                        "У инцидента отсутствуют звонки"));
                results.add(singleResult(incidentRef, "Операции и связи", OPERATION_POINTS, false,
                        "У инцидента отсутствуют звонки"));
                results.add(singleResult(incidentRef, "Обработка звонков", COVERAGE_POINTS, false,
                        "У инцидента отсутствуют звонки"));
                continue;
            }
            results.addAll(fieldResults(incidentRef, incident, calls, byDialup, callNumbers));
            results.addAll(operationResults(incidentRef, calls, byDialup, cards, callNumbers));
            results.addAll(coverageResults(incidentRef, calls, byDialup, callNumbers));
        }
        return results;
    }

    private static Map<String, Integer> callNumbers(List<IncidentContext> incidents,
                                                     List<SolutionContext> revisions) {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        for (SolutionContext revision : revisions) {
            if (!revision.getDialupId().isBlank()) {
                numbers.computeIfAbsent(revision.getDialupId(), ignored -> numbers.size() + 1);
            }
        }
        for (IncidentContext incident : incidents) {
            for (DialupContext call : calls(incident)) {
                numbers.computeIfAbsent(call.getId(), ignored -> numbers.size() + 1);
            }
        }
        return numbers;
    }

    private static void validateDialups(List<IncidentContext> incidents, Set<String> cardDialupIds) {
        Set<String> expectedDialupIds = new HashSet<>();
        for (IncidentContext incident : incidents) {
            for (DialupContext call : calls(incident)) {
                if (call.getId().isBlank() || !expectedDialupIds.add(call.getId())) {
                    throw new IllegalArgumentException("Missing or duplicate dialup ID");
                }
            }
        }
        if (!expectedDialupIds.containsAll(cardDialupIds)) {
            throw new IllegalArgumentException("Card references a dialup outside the level");
        }
    }

    private static List<CriterionResult> fieldResults(IncidentRef incidentRef, IncidentContext incident,
                                                       List<ExpectedCall> calls,
                                                       Map<String, List<SolutionContext>> byDialup,
                                                       Map<String, Integer> callNumbers) {
        List<WeightedCheck> checks = new ArrayList<>();
        for (ExpectedCall call : calls) {
            List<SolutionContext> matches = byDialup.getOrDefault(call.dialup().getId(), List.of());
            List<SolutionContext> candidates = matches.isEmpty()
                    ? List.of(SolutionContext.getDefaultInstance()) : matches;
            List<List<FieldCheck>> checksByCard = candidates.stream()
                    .map(card -> fields(incident, call, card)).toList();
            int totalChecks = checksByCard.stream().mapToInt(List::size).sum();
            double cardWeight = 1.0 / calls.size();
            if (totalChecks == 0) {
                checks.add(new WeightedCheck(!matches.isEmpty(), cardWeight,
                        callPrefix(call, callNumbers) + (!matches.isEmpty()
                                ? "Проверяемые поля для карточки не заданы"
                                : "Карточка отсутствует")));
                continue;
            }
            for (List<FieldCheck> cardChecks : checksByCard) {
                for (FieldCheck check : cardChecks) {
                    checks.add(new WeightedCheck(check.correct(), cardWeight / totalChecks,
                            callPrefix(call, callNumbers) + check.feedback()));
                }
            }
        }
        return allocate(incidentRef, "Поля", FIELD_POINTS, checks);
    }

    private static List<CriterionResult> operationResults(IncidentRef incidentRef, List<ExpectedCall> calls,
                                                           Map<String, List<SolutionContext>> byDialup,
                                                           Map<String, SolutionContext> cards,
                                                           Map<String, Integer> callNumbers) {
        List<WeightedCheck> checks = new ArrayList<>();
        for (ExpectedCall call : calls) {
            List<SolutionContext> matches = byDialup.getOrDefault(call.dialup().getId(), List.of());
            boolean correct = !matches.isEmpty() && matches.stream().allMatch(card ->
                    correctOperation(call, card, cards));
            String expected = operationName(call.operation());
            checks.add(new WeightedCheck(correct, 1.0 / calls.size(), callPrefix(call, callNumbers)
                    + (correct ? "Операция " + expected + " выполнена верно"
                    : "Операция или связь указана неверно, ожидалось: " + expected)));
        }
        return allocate(incidentRef, "Операции и связи", OPERATION_POINTS, checks);
    }

    private static List<CriterionResult> coverageResults(IncidentRef incidentRef, List<ExpectedCall> calls,
                                                          Map<String, List<SolutionContext>> byDialup,
                                                          Map<String, Integer> callNumbers) {
        List<WeightedCheck> checks = calls.stream().map(call -> {
            int count = byDialup.getOrDefault(call.dialup().getId(), List.of()).size();
            return new WeightedCheck(count == 1, 1.0 / calls.size(), callPrefix(call, callNumbers)
                    + (count == 1 ? "Создана одна карточка" : "Ожидалась одна карточка, найдено: " + count));
        }).toList();
        return allocate(incidentRef, "Обработка звонков", COVERAGE_POINTS, checks);
    }

    private static List<CriterionResult> allocate(IncidentRef incidentRef, String category, int budget,
                                                   List<WeightedCheck> checks) {
        int[] points = new int[checks.size()];
        int allocated = 0;
        for (int i = 0; i < checks.size(); i++) {
            points[i] = (int) Math.floor(budget * checks.get(i).weight());
            allocated += points[i];
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) order.add(i);
        order.sort(Comparator.<Integer>comparingDouble(i ->
                budget * checks.get(i).weight() - points[i]).reversed().thenComparingInt(i -> i));
        for (int i = 0; allocated < budget; i++, allocated++) points[order.get(i % order.size())]++;

        List<CriterionResult> results = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) {
            WeightedCheck check = checks.get(i);
            results.add(result(incidentRef, category, points[i], check.correct(), check.feedback()));
        }
        return results;
    }

    private static CriterionResult singleResult(IncidentRef incidentRef, String category, int max,
                                                  boolean correct, String feedback) {
        return result(incidentRef, category, max, correct, feedback);
    }

    private static CriterionResult result(IncidentRef incidentRef, String category, int max,
                                           boolean correct, String feedback) {
        CriterionResult result = new CriterionResult();
        result.setIncidentId(incidentRef.id());
        result.setIncidentOrder(incidentRef.order());
        result.setCriterionName(category);
        result.setMaxScore(max);
        result.setScore(correct ? max : 0);
        result.setFeedback(feedback);
        return result;
    }

    private static List<DialupContext> calls(IncidentContext incident) {
        return expectedCalls(incident).stream().map(ExpectedCall::dialup).toList();
    }

    private static List<ExpectedCall> expectedCalls(IncidentContext incident) {
        List<StageContext> stages = incident.getStagesList().stream()
                .sorted(Comparator.comparingInt(StageContext::getPosition))
                .toList();
        List<ExpectedCall> result = new ArrayList<>();
        DialupContext previousStageCanonical = null;
        for (StageContext stage : stages) {
            List<DialupContext> dialups = stage.getDialupsList().stream()
                    .sorted(Comparator.comparingInt(DialupContext::getPosition))
                    .toList();
            if (dialups.isEmpty()) continue;

            DialupContext currentStageCanonical = dialups.getFirst();
            result.add(new ExpectedCall(currentStageCanonical, stage,
                    previousStageCanonical == null ? ExpectedOperation.CREATE : ExpectedOperation.CREATE_CHILD,
                    previousStageCanonical == null ? "" : previousStageCanonical.getId()));
            for (int index = 1; index < dialups.size(); index++) {
                result.add(new ExpectedCall(dialups.get(index), stage, ExpectedOperation.DUPLICATE,
                        currentStageCanonical.getId()));
            }
            previousStageCanonical = currentStageCanonical;
        }
        return result;
    }

    private static boolean correctOperation(ExpectedCall expected, SolutionContext card,
                                            Map<String, SolutionContext> cards) {
        return switch (expected.operation()) {
            case CREATE -> card.getStatus() == SolutionContextStatus.ACTIVE
                    && card.getParentId().isEmpty() && card.getDuplicateOfId().isEmpty();
            case CREATE_CHILD -> card.getStatus() == SolutionContextStatus.ACTIVE
                    && card.getDuplicateOfId().isEmpty()
                    && linked(card, card.getParentId(), expected.targetDialupId(), cards);
            case DUPLICATE -> card.getStatus() == SolutionContextStatus.CLOSED_DUPLICATE
                    && card.getParentId().isEmpty()
                    && linked(card, card.getDuplicateOfId(), expected.targetDialupId(), cards);
        };
    }

    private static boolean linked(SolutionContext card, String targetId, String expectedDialup,
                                  Map<String, SolutionContext> cards) {
        SolutionContext target = cards.get(targetId);
        return target != null && !expectedDialup.isBlank() && !targetId.equals(card.getCardId())
                && target.getStatus() == SolutionContextStatus.ACTIVE
                && target.getDialupId().equals(expectedDialup);
    }

    private static List<FieldCheck> fields(IncidentContext incident, ExpectedCall call, SolutionContext card) {
        List<FieldCheck> checks = new ArrayList<>();
        DialupContext dialup = call.dialup();
        StageContext stage = call.stage();
        if (dialup.hasApplicant()) person(checks, dialup.getApplicant(), card.getApplicant(), "заявителя");
        if (stage.hasVictim()) person(checks, stage.getVictim(), card.getVictim(), "пострадавшего");
        stage.getAdditionalInfoList().forEach(info -> checks.add(field(
                same(info.getFieldValue(), card.getAdditionalInfoMap().get(info.getAdditionalInfoId())),
                "Поле «" + info.getFieldName() + "»")));
        if (!stage.getType().getId().isBlank()) checks.add(field(
                same(stage.getType().getId(), card.getIncidentType()), "Тип инцидента"));
        return checks;
    }

    private static void person(List<FieldCheck> checks, Applicant expected, PersonInfo actual, String role) {
        checks.add(field(same(expected.getFirstName(), actual.getFirstName()), "Имя " + role));
        checks.add(field(same(expected.getLastName(), actual.getLastName()), "Фамилия " + role));
        checks.add(field(same(expected.getMiddleName(), actual.getMiddleName()), "Отчество " + role));
        checks.add(field(same(expected.getPhone(), actual.getPhone()), "Телефон " + role));
        checks.add(field(same(expected.getContactPhone(), actual.getContactPhone()), "Контактный телефон " + role));
        checks.add(field(same(expected.getAddress(), actual.getAddress()), "Адрес " + role));
        checks.add(field(same(expected.getAdditionalInfo(), actual.getAdditionalInfo()),
                "Дополнительная информация " + role));
    }

    private static FieldCheck field(boolean correct, String name) {
        return new FieldCheck(correct, name + (correct ? " указано верно" : " указано неверно"));
    }

    private static String callPrefix(ExpectedCall call, Map<String, Integer> callNumbers) {
        return "Звонок " + callNumbers.get(call.dialup().getId()) + ": ";
    }

    private static String operationName(ExpectedOperation operation) {
        return switch (operation) {
            case CREATE -> "создать карточку";
            case CREATE_CHILD -> "создать дочернюю карточку";
            case DUPLICATE -> "отметить дубликат";
        };
    }

    private static boolean same(String expected, String actual) {
        return FieldValueComparator.hasSameContent(expected, actual);
    }

    static Map<String, SolutionContext> assemble(List<SolutionContext> revisions) {
        Map<String, List<SolutionContext>> grouped = new TreeMap<>();
        for (SolutionContext revision : revisions) {
            if (revision.getCardId().isBlank()) throw new IllegalArgumentException("Missing card ID");
            grouped.computeIfAbsent(revision.getCardId(), ignored -> new ArrayList<>()).add(revision);
        }
        Map<String, SolutionContext> cards = new TreeMap<>();
        grouped.forEach((id, history) -> {
            history.sort(Comparator.comparingLong(SolutionContext::getVersion));
            SolutionContext state = SolutionContext.getDefaultInstance();
            long previousVersion = -1;
            for (SolutionContext revision : history) {
                if (revision.getVersion() <= previousVersion) throw new IllegalArgumentException("Duplicate card version");
                if (previousVersion >= 0 && !state.getDialupId().equals(revision.getDialupId())) {
                    throw new IllegalArgumentException("Card changed dialup");
                }
                SolutionContext.Builder next = revision.toBuilder()
                        .setApplicant(merge(state.getApplicant(), revision.getApplicant()))
                        .setVictim(merge(state.getVictim(), revision.getVictim()));
                if (revision.getIncidentType().isBlank()) next.setIncidentType(state.getIncidentType());
                boolean mapProvided = revision.hasAdditionalInfoProvided()
                        ? revision.getAdditionalInfoProvided() : revision.getAdditionalInfoCount() > 0;
                if (!mapProvided) next.clearAdditionalInfo().putAllAdditionalInfo(state.getAdditionalInfoMap());
                state = next.build();
                previousVersion = revision.getVersion();
            }
            cards.put(id, state);
        });
        return cards;
    }

    private static PersonInfo merge(PersonInfo previous, PersonInfo next) {
        PersonInfo.Builder result = previous.toBuilder();
        next.getAllFields().forEach((field, value) -> {
            if (value instanceof String text && !text.isBlank()) result.setField(field, value);
        });
        return result.build();
    }

    private enum ExpectedOperation { CREATE, DUPLICATE, CREATE_CHILD }
    private record ExpectedCall(DialupContext dialup, StageContext stage, ExpectedOperation operation,
                                String targetDialupId) {}
    private record IncidentRef(String id, int order) {}
    private record FieldCheck(boolean correct, String feedback) {}
    private record WeightedCheck(boolean correct, double weight, String feedback) {}
}
