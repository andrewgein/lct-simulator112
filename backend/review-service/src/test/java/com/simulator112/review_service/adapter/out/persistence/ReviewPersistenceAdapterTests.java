package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReviewPersistenceAdapterTests {
    @Autowired
    private ReviewStore store;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roundTripsDomainReview() {
        UUID contextId = UUID.randomUUID();
        Review saved = store.save(new Review(contextId, UUID.randomUUID(), UUID.randomUUID(),
                ReviewStatus.IN_REVIEW, List.of(new CriterionResult("incident", 1,
                "Поля", 60, 70, "Проверка")), 60, 60, 70,
                10, 30, 0, null, null, null, null, null, 40, 60, 80));
        entityManager.flush();
        entityManager.clear();

        Review restored = store.findByContextId(contextId).orElseThrow();

        assertThat(saved.contextId()).isEqualTo(contextId);
        assertThat(restored.results()).hasSize(1);
        assertThat(restored.results().getFirst().score()).isEqualTo(60);
        assertThat(restored.threshold3()).isEqualTo(40);
        assertThat(restored.threshold4()).isEqualTo(60);
        assertThat(restored.threshold5()).isEqualTo(80);
        assertThat(restored.grade()).isEqualTo(5);
        assertThat(restored.passed()).isTrue();
    }
}
