package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DdsReviewRubric implements ReviewRubric {
    private final Rubric rubric = new RubricBuilder(ReviewSubmission.TargetType.DDS)
            .addStage(new Stage("Обработка инцидента ДДС", List.of(
                    criterion("Своевременность статусов и звонки", 100))))
            .build();

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
                return submission.incidents().stream().flatMap(incident -> {
                    int stageBudget = incident.stages().stream()
                            .anyMatch(stage -> stage.expectedComment() != null && !stage.expectedComment().isBlank())
                            ? maxScore() - 20 : maxScore();
                    var runtime = submission.runtime().stream()
                            .filter(value -> value.incidentId().equals(incident.id())).findFirst().orElse(null);
                    if (runtime == null) {
                        return List.of(new CriterionResult(incident.id(), incident.order(), category, 0, stageBudget,
                                "Runtime-прогресс инцидента отсутствует")).stream();
                    }
                    var milestones = incident.stages().stream().filter(stage -> stage.actualStatus() != null).toList();
                    var calls = incident.stages().stream().filter(stage -> !stage.calls().isEmpty()).toList();
                    List<WeightedCheck> checks = new ArrayList<>();
                    if (!milestones.isEmpty() || !calls.isEmpty()) {
                        double weight = 1.0 / (milestones.size() + calls.stream().mapToInt(stage -> stage.calls().size()).sum());
                        for (var milestone : milestones) {
                            var stage = runtime.stages().stream().filter(value -> value.stageId().equals(milestone.id()))
                                    .findFirst().orElse(null);
                            var nextStage = runtime.stages().stream()
                                    .filter(value -> stage != null && !value.stageId().equals(milestone.id())
                                            && value.startedAt() != null && stage.startedAt() != null
                                            && value.startedAt().isAfter(stage.startedAt()))
                                    .min(Comparator.comparing(ReviewSubmission.StageRuntime::startedAt)).orElse(null);
                            var changedAt = nextStage != null ? nextStage.startedAt()
                                    : stage == null ? null : stage.deadline() != null && (submission.submittedAt() == null || !stage.deadline().isAfter(submission.submittedAt()))
                                            ? stage.deadline() : stage.startedAt();
                            var dueAt = nextStage != null && nextStage.deadline() != null
                                    ? nextStage.deadline() : submission.submittedAt();
                            var reported = runtime.reactionEvents().stream()
                                    .filter(event -> changedAt != null && dueAt != null && event.changedAt() != null
                                            && !event.changedAt().isBefore(changedAt)
                                            && !event.changedAt().isAfter(dueAt))
                                    .max(Comparator.comparing(ReviewSubmission.ReactionEvent::changedAt)).orElse(null);
                            boolean correct = reported != null && milestone.actualStatus() == reported.status();
                            checks.add(new WeightedCheck(correct, weight,
                                    "Статус " + milestone.actualStatus() + " возник " + changedAt
                                            + ", до " + dueAt + " диспетчер указал "
                                            + (reported == null ? "не указан" : reported.status()) + "."));
                        }
                        for (var callStage : calls) {
                            var runtimeStage = runtime.stages().stream().filter(value -> value.stageId().equals(callStage.id()))
                                    .findFirst().orElse(null);
                            for (var call : callStage.calls()) {
                                boolean completed = runtimeStage != null && runtimeStage.completedCallIds().contains(call.id());
                                checks.add(new WeightedCheck(completed, weight, completed
                                        ? "Звонок " + call.id() + " состоялся."
                                        : "Звонок " + call.id() + " пропущен."));
                            }
                        }
                    } else {
                        checks.add(new WeightedCheck(false, 1, "Фактические статусы и звонки в сценарии ДДС не заданы."));
                    }
                    if (submission.startedAt() != null && submission.submittedAt() != null
                            && !submission.submittedAt().isBefore(submission.startedAt())) {
                        long duration = Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
                        long limit = 30L * Math.max(1, submission.incidents().size());
                        if (duration > limit) checks.add(new WeightedCheck(false, 0,
                                "Норматив превышен: " + duration + " сек. при нормативе " + limit + " сек."));
                    }
                    return allocate(incident, category, stageBudget, checks).stream();
                }).toList();
            }
        };
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

    private record WeightedCheck(boolean correct, double weight, String feedback) {
    }
}
