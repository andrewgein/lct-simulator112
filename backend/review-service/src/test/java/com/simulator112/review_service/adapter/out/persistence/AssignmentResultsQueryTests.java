package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.in.GetAssignmentResultsUseCase;
import com.simulator112.review_service.domain.model.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AssignmentResultsQueryTests {
    private final SpringDataReviewRepository repository = mock(SpringDataReviewRepository.class);
    private final AssignmentResultsQuery query = new AssignmentResultsQuery(repository);

    @Test
    void returnsLatestResultForEachAssignment() {
        var user = UUID.randomUUID();
        var assignment = UUID.randomUUID();
        var missing = UUID.randomUUID();
        when(repository.findAllByUserIdAndAssignmentIdInOrderByCreatedAtDesc(user, List.of(assignment, missing)))
                .thenReturn(List.of(review(assignment, 75), review(assignment, 40)));

        assertThat(query.get(user, List.of(assignment, missing)))
                .containsExactly(new GetAssignmentResultsUseCase.Result(assignment, 75, 100, 4));
    }

    @Test
    void olderCompletedResultIsNotUsedWhenLatestResultIsPending() {
        var user = UUID.randomUUID();
        var assignment = UUID.randomUUID();
        var pending = review(assignment, 80);
        pending.setStatus(ReviewStatus.IN_REVIEW);
        when(repository.findAllByUserIdAndAssignmentIdInOrderByCreatedAtDesc(user, List.of(assignment)))
                .thenReturn(List.of(pending, review(assignment, 90)));

        assertThat(query.get(user, List.of(assignment))).isEmpty();
    }

    private ReviewJpaEntity review(UUID assignmentId, int score) {
        var entity = new ReviewJpaEntity();
        entity.setAssignmentId(assignmentId);
        entity.setStatus(ReviewStatus.DONE);
        entity.setAutomaticScore(score);
        entity.setFinalScore(score);
        entity.setMaxScore(100);
        entity.setThreshold3(40);
        entity.setThreshold4(60);
        entity.setThreshold5(80);
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
