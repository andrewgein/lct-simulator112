package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import com.simulator112.review_service.application.event.ReviewResultChanged;
import org.springframework.context.ApplicationEventPublisher;
import com.simulator112.review_service.application.port.in.ConfirmReviewUseCase;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.application.port.in.SubmitReviewUseCase;
import com.simulator112.review_service.application.port.out.DialogueAnalysisPort;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.evaluation.ReviewRubric;
import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import com.simulator112.review_service.domain.model.ReviewSubmission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewApplicationService implements SubmitReviewUseCase, GetReviewUseCase, ConfirmReviewUseCase {
    private static final long DEFAULT_TIME_LIMIT_SECONDS = 30;

    private final ReviewStore store;
    private final List<ReviewRubric> rubrics;
    private final DialogueAnalysisPort dialogueAnalysisPort;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public Review submit(ReviewSubmission submission) {
        ReviewRubric rubric = rubrics.stream().filter(value -> value.supports(submission)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Не найдена рубрика для " + submission.targetType()));
        long duration = durationSeconds(submission);
        long timeLimit = DEFAULT_TIME_LIMIT_SECONDS * Math.max(1, submission.incidents().size());
        var results = new ArrayList<>(rubric.evaluate(submission));
        results.addAll(evaluateDialogue(submission));
        results.addAll(evaluateDdsComments(submission));
        int score = results.stream().mapToInt(value -> value.score()).sum();
        int maxScore = results.stream().mapToInt(value -> value.maxScore()).sum();
        Review review = new Review(submission.contextId(), submission.userId(), submission.assignmentId(),
                ReviewStatus.DONE, results, score, score, maxScore, duration, timeLimit,
                Math.max(0, duration - timeLimit), null, null, null, null, null,
                submission.threshold3(), submission.threshold4(), submission.threshold5());
        Review saved = store.save(review);
        events.publishEvent(new ReviewResultChanged(saved.userId(), saved.assignmentId()));
        return saved;
    }

    @Override
    @Transactional
    public Review confirm(UUID contextId, UUID expertId, Integer finalScore, String comment) {
        Review review = getByContextId(contextId);
        if (review.status() != ReviewStatus.DONE) {
            throw new IllegalStateException("Автоматическая проверка ещё не завершена");
        }
        Review saved = store.save(review.confirm(expertId, finalScore, comment, Instant.now()));
        events.publishEvent(new ReviewResultChanged(saved.userId(), saved.assignmentId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Review getByContextId(UUID contextId) {
        return store.findByContextId(contextId)
                .orElseThrow(() -> new ReviewNotFoundException(contextId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getByUserId(UUID userId) {
        return store.findByUserId(userId);
    }

    private List<CriterionResult> evaluateDialogue(ReviewSubmission submission) {
        if (submission.targetType() != ReviewSubmission.TargetType.SYSTEM_112) return List.of();
        var criteria = submission.incidents().stream()
                .flatMap(incident -> incident.criteria().dialogueCriteria().stream())
                .toList();
        if (criteria.isEmpty()) return List.of();

        var analysisByCriterion = dialogueAnalysisPort.analyze(submission.transcript(), criteria).stream()
                .collect(Collectors.toMap(DialogueAnalysisPort.DialogueAnalysis::criterionId, Function.identity()));
        return submission.incidents().stream().flatMap(incident -> incident.criteria().dialogueCriteria().stream()
                .map(criterion -> {
                    var analysis = analysisByCriterion.get(criterion.id());
                    if (analysis == null) {
                        throw new IllegalStateException("MLServer не вернул результат для критерия " + criterion.id());
                    }
                    String confidence = String.format(Locale.ROOT, "%.3f", analysis.confidence());
                    String feedback = analysis.matched()
                            ? "Критерий оценки диалога выполнен. Уверенность модели: " + confidence + "."
                            : "Критерий оценки диалога не выполнен. Уверенность модели: " + confidence + ".";
                    return new CriterionResult(incident.id(), incident.order(), criterion.name(),
                            analysis.matched() ? criterion.weight() : 0, criterion.weight(), feedback);
                })).toList();
    }

    private List<CriterionResult> evaluateDdsComments(ReviewSubmission submission) {
        if (submission.targetType() != ReviewSubmission.TargetType.DDS) return List.of();
        var results = new ArrayList<CriterionResult>();
        for (var incident : submission.incidents()) {
            var stages = incident.stages().stream().filter(stage -> stage.expectedComment() != null
                    && !stage.expectedComment().isBlank()).toList();
            if (stages.isEmpty()) continue;
            var runtime = submission.runtime().stream().filter(value -> value.incidentId().equals(incident.id()))
                    .findFirst().orElse(null);
            for (int index = 0; index < stages.size(); index++) {
                var stage = stages.get(index);
                int points = 20 / stages.size() + (index < 20 % stages.size() ? 1 : 0);
                var result = runtime == null ? null : runtime.stages().stream()
                        .filter(value -> value.stageId().equals(stage.id())).findFirst().orElse(null);
                String comment = result == null ? null : result.comment();
                boolean matched = false;
                String feedback = "Комментарий по звонку отсутствует.";
                if (comment != null && !comment.isBlank()) {
                    var criterion = new ReviewSubmission.DialogueCriterion(stage.id(), "Комментарий по звонку",
                            stage.expectedComment(), 1);
                    var analysis = dialogueAnalysisPort.analyze(
                            List.of(new ReviewSubmission.TranscriptPhrase("USER", comment)), List.of(criterion)).getFirst();
                    matched = analysis.matched();
                    feedback = (matched ? "Комментарий соответствует ожидаемым сведениям."
                            : "Комментарий не соответствует ожидаемым сведениям.") + " Комментарий: " + comment;
                }
                results.add(new CriterionResult(incident.id(), incident.order(), "Комментарий по звонку",
                        matched ? points : 0, points, feedback));
            }
        }
        return results;
    }

    private long durationSeconds(ReviewSubmission submission) {
        if (submission.startedAt() == null || submission.submittedAt() == null
                || submission.submittedAt().isBefore(submission.startedAt())) return 0;
        return Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
    }
}
