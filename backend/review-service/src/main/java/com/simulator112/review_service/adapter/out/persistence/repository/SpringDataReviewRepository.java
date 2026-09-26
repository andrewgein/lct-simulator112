package com.simulator112.review_service.adapter.out.persistence.repository;

import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface SpringDataReviewRepository extends JpaRepository<ReviewJpaEntity, UUID>, JpaSpecificationExecutor<ReviewJpaEntity> {
    List<ReviewJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
