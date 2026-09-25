package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewNotFoundException;
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

    @Override
    @Transactional
    public Review submit(ReviewSubmission submission) {
        ReviewRubric rubric = rubrics.stream().filter(value -> value.supports(submission)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Не найдена рубрика для " + submission.targetType()));
        long duration = durationSeconds(submission);
        long timeLimit = DEFAULT_TIME_LIMIT_SECONDS * Math.max(1, submission.incidents().size());
        var results = new ArrayList<>(rubric.evaluate(submission));
        results.addAll(evaluateDialogue(submission));
        int score = results.stream().mapToInt(value -> value.score()).sum();
        int maxScore = results.stream().mapToInt(value -> value.maxScore()).sum();
        Review review = new Review(submission.contextId(), submission.userId(), submission.assignmentId(),
                ReviewStatus.DONE, results, score, score, maxScore, duration, timeLimit,
                Math.max(0, duration - timeLimit), null, null, null, null, null);
        return store.save(review);
    }

    @Override
    @Transactional
    public Review confirm(UUID contextId, UUID expertId, Integer finalScore, String comment) {
        Review review = getByContextId(contextId);
        if (review.status() != ReviewStatus.DONE) {
            throw new IllegalStateException("Автоматическая проверка ещё не завершена");
        }
        return store.save(review.confirm(expertId, finalScore, comment, Instant.now()));
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

    private long durationSeconds(ReviewSubmission submission) {
        if (submission.startedAt() == null || submission.submittedAt() == null
                || submission.submittedAt().isBefore(submission.startedAt())) return 0;
        return Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
    }
}
