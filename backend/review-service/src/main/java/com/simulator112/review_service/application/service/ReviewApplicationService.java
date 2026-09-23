package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.application.port.in.SubmitReviewUseCase;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.evaluation.ReviewRubric;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import com.simulator112.review_service.domain.model.ReviewSubmission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewApplicationService implements SubmitReviewUseCase, GetReviewUseCase {
    private final ReviewStore store;
    private final List<ReviewRubric> rubrics;

    @Override
    @Transactional
    public Review submit(ReviewSubmission submission) {
        ReviewRubric rubric = rubrics.stream().filter(value -> value.supports(submission)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Не найдена рубрика для " + submission.targetType()));
        Review review = new Review(submission.contextId(), submission.userId(), submission.assignmentId(),
                ReviewStatus.DONE, rubric.evaluate(submission), null, null);
        return store.save(review);
    }

    @Override
    @Transactional
    public Review getByContextId(UUID contextId) {
        return store.findByContextId(contextId)
                .orElseThrow(() -> new IllegalArgumentException("Проверка не найдена: " + contextId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getByUserId(UUID userId) {
        return store.findByUserId(userId);
    }
}
