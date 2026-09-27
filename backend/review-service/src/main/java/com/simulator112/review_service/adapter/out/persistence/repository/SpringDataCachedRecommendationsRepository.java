package com.simulator112.review_service.adapter.out.persistence.repository;

import com.simulator112.review_service.adapter.out.persistence.entity.CachedRecommendationsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCachedRecommendationsRepository extends JpaRepository<CachedRecommendationsJpaEntity, UUID> {
}
