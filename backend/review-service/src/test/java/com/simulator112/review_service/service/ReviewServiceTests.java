package com.simulator112.review_service.service;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.incident.grpc.contract.LevelContext;
import com.simulator112.review_service.model.entity.Review;
import com.simulator112.review_service.repository.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTests {
    @Test
    void persistsContextUserAndLevelIdentifiers() {
        ReviewRepository repository = mock(ReviewRepository.class);
        when(repository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID contextId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var context = FullContext.newBuilder().setUuid(contextId.toString()).setUserId(userId.toString())
                .setLevelContext(LevelContext.newBuilder().setId("level-id")).build();
        Review review = new ReviewService(repository, new Rubric(List.of())).review(context);
        assertEquals(contextId, review.getContextId());
        assertEquals(userId, review.getUserId());
        assertEquals("level-id", review.getLevelId());
        verify(repository).save(review);
    }
}
