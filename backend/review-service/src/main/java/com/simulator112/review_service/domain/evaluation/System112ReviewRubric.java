package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.time.Duration;
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
                current.mainCardId(), merge(previous.applicant(), current.applicant()),
                current.victimCount() == null ? previous.victimCount() : current.victimCount(),
                current.additionalInfoProvided() ? current.additionalInfo() : previous.additionalInfo(),
                current.additionalInfoProvided(), current.incidentTypes().isEmpty() ? previous.incidentTypes() : current.incidentTypes(),
                current.services().isEmpty() ? previous.services() : current.services(),
                current.createdAt() == null ? previous.createdAt() : current.createdAt());
    }

    private static ReviewSubmission.Person merge(ReviewSubmission.Person previous, ReviewSubmission.Person current) {
        if (current == null) return previous;
        if (previous == null) return current;
        return new ReviewSubmission.Person(inherit(current.firstName(), previous.firstName()), inherit(current.lastName(), previous.lastName()),
                inherit(current.middleName(), previous.middleName()), inherit(current.phone(), previous.phone()),
                inherit(current.contactPhone(), previous.contactPhone()), inherit(current.onScenePhone(), previous.onScenePhone()),
                inherit(current.address(), previous.address()),
                inherit(current.additionalInfo(), previous.additionalInfo()));
    }

    private static boolean same(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private static boolean sameSet(List<String> expected, List<String> actual) {
        Set<String> expectedCodes = new HashSet<>();
        expected.forEach(value -> expectedCodes.add(normalize(value)));
        Set<String> actualCodes = new HashSet<>();
        actual.forEach(value -> actualCodes.add(normalize(value)));
        return expectedCodes.equals(actualCodes);
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
                return submission.incidents().stream().flatMap(incident -> {
                    List<ExpectedCall> calls = expectedCalls(incident);
                    List<WeightedCheck> checks = switch (category) {
                        case "Поля" -> fieldChecks(calls, byCall);
                        case "Операции и связи" -> operationChecks(calls, byCall, cards);
                        case "Обработка звонков" -> callChecks(submission, incident, calls, byCall);
                        default -> throw new IllegalStateException("Неизвестный критерий: " + category);
                    };
                    return allocate(incident, category, categoryBudget(incident, category), checks).stream();
                }).toList();
            }
        };
    }

    private List<WeightedCheck> fieldChecks(List<ExpectedCall> calls,
                                            Map<String, List<ReviewSubmission.CardRevision>> byCall) {
        if (calls.isEmpty()) return List.of(new WeightedCheck(false, 1, "В сценарии отсутствуют звонки для проверки."));
        List<WeightedCheck> result = new ArrayList<>();
        double callWeight = 1.0 / calls.size();
        for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
            ExpectedCall expected = calls.get(callIndex);
            var cards = byCall.getOrDefault(expected.call().id(), List.of());
            var card = cards.isEmpty() ? null : cards.getFirst();
            List<AtomicCheck> checks = new ArrayList<>();
            if (expected.call().person() != null) {
                personChecks(checks, expected.call().person(), card == null ? null : card.applicant());
            }
            int victimCount = expected.stage().victimCount();
            checks.add(new AtomicCheck(card != null && card.victimCount() != null
                    && victimCount == card.victimCount(), card == null || card.victimCount() == null
                    ? "Количество пострадавших не указано." : "Количество пострадавших указано неверно.",
                    "Количество пострадавших указано верно."));
            var classifierCodes = expected.stage().classifierCodes();
            if (!classifierCodes.isEmpty()) {
                checks.add(new AtomicCheck(card != null && sameSet(classifierCodes, card.incidentTypes()),
                        card == null || card.incidentTypes().isEmpty()
                                ? "Классификация не указана." : "Классификация выбрана неверно.",
                        "Классификация выбрана верно."));
            }
            if (checks.isEmpty()) {
                checks.add(new AtomicCheck(card != null, "Карточка по звонку отсутствует.", "Карточка создана."));
            }
            double checkWeight = callWeight / checks.size();
            String prefix = "Звонок №" + (callIndex + 1) + ": ";
            checks.forEach(check -> result.add(new WeightedCheck(check.correct(), checkWeight,
                    prefix + (check.correct() ? check.successFeedback() : check.failureFeedback()))));
        }
        return result;
    }

    private List<WeightedCheck> operationChecks(List<ExpectedCall> calls,
                                                 Map<String, List<ReviewSubmission.CardRevision>> byCall,
                                                 Map<String, ReviewSubmission.CardRevision> cards) {
        if (calls.isEmpty()) return List.of(new WeightedCheck(false, 1, "В сценарии отсутствуют операции для проверки."));
        List<WeightedCheck> result = new ArrayList<>();
        double weight = 1.0 / calls.size();
        for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
            ExpectedCall expected = calls.get(callIndex);
            String callLabel = "звонка №" + (callIndex + 1);
            var matches = byCall.getOrDefault(expected.call().id(), List.of());
            boolean valid = false;
            String feedback;
            if (matches.isEmpty()) {
                feedback = "Для " + callLabel + " карточка не создана.";
            } else if (matches.size() > 1) {
                feedback = "Для " + callLabel + " создано несколько карточек.";
            } else {
                var card = matches.getFirst();
                valid = switch (expected.operation()) {
                    case CREATE -> blank(card.mainCardId());
                    case LINK -> linked(card.mainCardId(), expected.targetCallId(), cards);
                };
                feedback = valid ? "Операция с карточкой " + callLabel + " выполнена верно."
                        : "Операция или связь карточки " + callLabel + " выполнена неверно.";
            }
            result.add(new WeightedCheck(valid, weight, feedback));
        }
        return result;
    }

    private boolean linked(String targetCardId, String expectedCallId, Map<String, ReviewSubmission.CardRevision> cards) {
        var target = cards.get(targetCardId);
        return target != null && expectedCallId.equals(target.callId());
    }

    private List<WeightedCheck> callChecks(ReviewSubmission submission, ReviewSubmission.IncidentScenario incident,
                                           List<ExpectedCall> calls,
                                           Map<String, List<ReviewSubmission.CardRevision>> byCall) {
        List<WeightedCheck> result = new ArrayList<>();
        if (calls.isEmpty()) {
            result.add(new WeightedCheck(false, 1, "В сценарии отсутствуют звонки для проверки."));
        } else {
            double weight = 1.0 / calls.size();
            for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
                ExpectedCall call = calls.get(callIndex);
                String callLabel = "Звонок №" + (callIndex + 1);
                int count = byCall.getOrDefault(call.call().id(), List.of()).size();
                String feedback = count == 1 ? callLabel + " обработан."
                        : count == 0 ? callLabel + " не обработан."
                        : "Для звонка №" + (callIndex + 1) + " найдено несколько карточек.";
                result.add(new WeightedCheck(count == 1, weight, feedback));
            }
        }
        if (submission.startedAt() != null && submission.submittedAt() != null
                && !submission.submittedAt().isBefore(submission.startedAt())) {
            long duration = Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
            long limit = 30L * Math.max(1, submission.incidents().size());
            if (duration > limit) result.add(new WeightedCheck(false, 0,
                    "Норматив превышен: " + duration + " сек. при нормативе " + limit + " сек."));
        }
        return result;
    }

    private List<CriterionResult> allocate(ReviewSubmission.IncidentScenario incident, String category,
                                           int budget, List<WeightedCheck> checks) {
        int[] points = new int[checks.size()];
        int allocated = 0;
        for (int index = 0; index < checks.size(); index++) {
            points[index] = (int) Math.floor(budget * checks.get(index).weight());
            allocated += points[index];
        }
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < checks.size(); index++) order.add(index);
        order.sort(Comparator.<Integer>comparingDouble(index ->
                budget * checks.get(index).weight() - points[index]).reversed().thenComparingInt(index -> index));
        for (int index = 0; allocated < budget; index++, allocated++) {
            points[order.get(index % order.size())]++;
        }
        List<CriterionResult> results = new ArrayList<>();
        for (int index = 0; index < checks.size(); index++) {
            WeightedCheck check = checks.get(index);
            int max = points[index];
            results.add(new CriterionResult(incident.id(), incident.order(), category,
                    check.correct() ? max : 0, max, check.feedback()));
        }
        return results;
    }

    private int categoryBudget(ReviewSubmission.IncidentScenario incident, String category) {
        int dialogueBudget = incident.criteria().dialogueCriteria().stream()
                .mapToInt(ReviewSubmission.DialogueCriterion::weight).sum();
        int[] budgets = ScoreBudget.scale(100 - dialogueBudget, 70, 20, 10);
        return switch (category) {
            case "Поля" -> budgets[0];
            case "Операции и связи" -> budgets[1];
            case "Обработка звонков" -> budgets[2];
            default -> throw new IllegalStateException("Неизвестный критерий: " + category);
        };
    }

    private List<ExpectedCall> expectedCalls(ReviewSubmission.IncidentScenario incident) {
        List<ExpectedCall> result = new ArrayList<>();
        String previousCanonical = null;
        for (var stage : incident.stages().stream().sorted(Comparator.comparing(
                value -> value.position() == null ? Integer.MAX_VALUE : value.position())).toList()) {
            var calls = stage.calls().stream().sorted(Comparator.comparingInt(ReviewSubmission.CallScenario::position)).toList();
            if (calls.isEmpty()) continue;
            String canonical = calls.getFirst().id();
            result.add(new ExpectedCall(calls.getFirst(), stage,
                    previousCanonical == null ? Operation.CREATE : Operation.LINK, previousCanonical));
            for (int index = 1; index < calls.size(); index++) {
                result.add(new ExpectedCall(calls.get(index), stage, Operation.LINK, canonical));
            }
            previousCanonical = canonical;
        }
        return result;
    }

    private void personChecks(List<AtomicCheck> checks, ReviewSubmission.Person expected,
                              ReviewSubmission.Person actual) {
        fieldCheck(checks, "Имя", expected.firstName(), actual == null ? null : actual.firstName());
        fieldCheck(checks, "Фамилия", expected.lastName(), actual == null ? null : actual.lastName());
        fieldCheck(checks, "Отчество", expected.middleName(), actual == null ? null : actual.middleName());
        fieldCheck(checks, "Телефон", expected.phone(), actual == null ? null : actual.phone());
        fieldCheck(checks, "Контактный телефон", expected.contactPhone(),
                actual == null ? null : actual.contactPhone());
        fieldCheck(checks, "Телефон на месте", expected.onScenePhone(),
                actual == null ? null : actual.onScenePhone());
        fieldCheck(checks, "Адрес", expected.address(), actual == null ? null : actual.address());
        fieldCheck(checks, "Дополнительная информация", expected.additionalInfo(),
                actual == null ? null : actual.additionalInfo());
    }

    private void fieldCheck(List<AtomicCheck> checks, String field, String expected, String actual) {
        if (blank(expected)) return;
        checks.add(new AtomicCheck(same(expected, actual), actual == null || actual.isBlank()
                ? field + " не указано." : field + " указано неверно.", field + " указано верно."));
    }

    private enum Operation {CREATE, LINK}

    private record AtomicCheck(boolean correct, String failureFeedback, String successFeedback) {
    }

    private record WeightedCheck(boolean correct, double weight, String feedback) {
    }

    private record ExpectedCall(ReviewSubmission.CallScenario call, ReviewSubmission.StageScenario stage,
                                Operation operation, String targetCallId) {
    }
}
