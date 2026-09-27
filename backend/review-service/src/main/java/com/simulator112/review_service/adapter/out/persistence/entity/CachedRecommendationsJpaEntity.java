package com.simulator112.review_service.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cached_recommendations")
@Getter
@Setter
public class CachedRecommendationsJpaEntity {
    @Id
    private UUID userId;
    @Column(nullable = false, columnDefinition = "text")
    private String recommendations;
    @Column(nullable = false)
    private int reviewsCount;
    @Column(nullable = false)
    private Instant generatedAt;
}
