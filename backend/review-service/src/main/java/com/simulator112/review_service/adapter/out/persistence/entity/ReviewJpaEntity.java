package com.simulator112.review_service.adapter.out.persistence.entity;

import com.simulator112.review_service.domain.model.ReviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class ReviewJpaEntity {
    @Id
    private UUID contextId;
    private UUID userId;
    private UUID levelId;
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "review_id")
    @OrderBy("incidentOrder ASC, criterionName ASC")
    private List<CriterionResultJpaEntity> results = new ArrayList<>();
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
