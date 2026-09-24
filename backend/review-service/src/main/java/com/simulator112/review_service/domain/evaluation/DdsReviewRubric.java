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
                    criterion("Выполнение этапов ДДС", 80),
                    criterion("Завершение инцидента ДДС", 20))))
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
                    var runtime = submission.runtime().stream()
                            .filter(value -> value.incidentId().equals(incident.id())).findFirst().orElse(null);
                    if (runtime == null) {
                        return List.of(result(incident, 0, category.equals("Выполнение этапов ДДС")
                                ? "Runtime-прогресс инцидента отсутствует"
                                : "Результат инцидента отсутствует")).stream();
                    }
                    if (category.equals("Завершение инцидента ДДС")) {
                        boolean completed = "COMPLETED".equals(runtime.status());
                        String feedback = completed ? "Инцидент успешно завершён."
                                : "Инцидент завершён с ошибкой: " + runtime.status() + ".";
                        return List.of(result(incident, completed ? maxScore() : 0, feedback)).stream();
                    }
                    var executed = runtime.stages().stream().filter(stage -> !"PENDING".equals(stage.status())).toList();
                    List<WeightedCheck> checks = new ArrayList<>();
                    if (executed.isEmpty()) {
                        checks.add(new WeightedCheck(false, 1, "Выполненные этапы ДДС отсутствуют."));
                    } else {
                        double weight = 1.0 / executed.size();
                        executed.forEach(stage -> checks.add(new WeightedCheck("SUCCEEDED".equals(stage.status()), weight,
                                "SUCCEEDED".equals(stage.status())
                                        ? "Этап " + stage.stageType() + " выполнен успешно."
                                        : "Этап " + stage.stageType() + " завершён со статусом " + stage.status() + ".")));
                    }
                    if (submission.startedAt() != null && submission.submittedAt() != null
                            && !submission.submittedAt().isBefore(submission.startedAt())) {
                        long duration = Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
                        long limit = 30L * Math.max(1, submission.incidents().size());
                        if (duration > limit) checks.add(new WeightedCheck(false, 0,
                                "Норматив превышен: " + duration + " сек. при нормативе " + limit + " сек."));
                    }
                    return allocate(incident, category, maxScore(), checks).stream();
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
