package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewComment;
import com.simulator112.review_service.domain.model.ReviewStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReviewCommentPersistenceAdapterTests {
    @Autowired
    private ReviewStore reviewStore;
    @Autowired
    private ReviewCommentStore commentStore;
    @Autowired
    private EntityManager entityManager;

    @Test
    void storesMultipleCommentsInCreationOrder() {
        UUID contextId = UUID.randomUUID();
        reviewStore.save(new Review(contextId, UUID.randomUUID(), UUID.randomUUID(), ReviewStatus.DONE,
                List.of(), 0, 0, 0, 0, 30, 0, null, null, null, null, null));
        ReviewComment second = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(), "Второй",
                Instant.parse("2026-01-01T11:00:00Z"));
        ReviewComment first = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(), "Первый",
                Instant.parse("2026-01-01T10:00:00Z"));
        commentStore.save(second);
        commentStore.save(first);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentStore.findByContextId(contextId)).extracting(ReviewComment::text)
                .containsExactly("Первый", "Второй");
    }
}
