package com.simulator112.review_service.adapter.out.persistence.repository;

import com.simulator112.review_service.adapter.out.persistence.entity.ReviewCommentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataReviewCommentRepository extends JpaRepository<ReviewCommentJpaEntity, UUID> {
    List<ReviewCommentJpaEntity> findAllByReviewContextIdOrderByCreatedAtAsc(UUID contextId);
}
