package com.simulator112.review_service.adapter.out.persistence.repository;

import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataReviewRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    List<ReviewJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ReviewJpaEntity> findAllByUserIdAndAssignmentIdInOrderByCreatedAtDesc(UUID userId, List<UUID> assignmentIds);
}
