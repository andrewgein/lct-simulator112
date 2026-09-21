package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

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
                return submission.incidents().stream().map(incident -> {
                    var runtime = submission.runtime().stream()
                            .filter(value -> value.incidentId().equals(incident.id())).findFirst().orElse(null);
                    if (runtime == null) {
                        return result(incident, 0, category.equals("Выполнение этапов ДДС")
                                ? "Runtime-прогресс инцидента отсутствует" : "Результат инцидента отсутствует");
                    }
                    if (category.equals("Завершение инцидента ДДС")) {
                        boolean completed = "COMPLETED".equals(runtime.status());
                        return result(incident, completed ? maxScore() : 0, completed
                                ? "Инцидент успешно завершён" : "Инцидент завершён с ошибкой: " + runtime.status());
                    }
                    var executed = runtime.stages().stream().filter(stage -> !"PENDING".equals(stage.status())).toList();
                    long successful = executed.stream().filter(stage -> "SUCCEEDED".equals(stage.status())).count();
                    int score = executed.isEmpty() ? 0 : (int) Math.round((double) maxScore() * successful / executed.size());
                    return result(incident, score, "Успешно выполнено этапов: " + successful + " из " + executed.size());
                }).toList();
            }
        };
    }
}
