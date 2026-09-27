package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.in.GetAssignmentResultsUseCase;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssignmentResultsQuery implements GetAssignmentResultsUseCase {
    private final SpringDataReviewRepository reviews;

    @Override
    @Transactional(readOnly = true)
    public List<Result> get(UUID userId, List<UUID> assignmentIds) {
        if (userId == null || assignmentIds == null || assignmentIds.isEmpty()
                || assignmentIds.size() > 500 || assignmentIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("Некорректный запрос результатов заданий");
        }
        var seen = new HashSet<UUID>();
        return reviews.findAllByUserIdAndAssignmentIdInOrderByCreatedAtDesc(userId, assignmentIds).stream()
                .filter(review -> seen.add(review.getAssignmentId()))
                .filter(review -> review.getStatus() == ReviewStatus.DONE)
                .map(review -> {
                    int score = review.getFinalScore() == null ? review.getAutomaticScore() : review.getFinalScore();
                    Integer grade = review.getMaxScore() <= 0 ? null : Review.gradeFor(score, review.getMaxScore(),
                            review.getThreshold3(), review.getThreshold4(), review.getThreshold5());
                    return new Result(review.getAssignmentId(), score, review.getMaxScore(), grade);
                }).toList();
    }
}
