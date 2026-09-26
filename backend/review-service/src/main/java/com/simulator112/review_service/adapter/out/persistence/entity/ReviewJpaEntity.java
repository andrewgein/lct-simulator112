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
    private UUID assignmentId;
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("incidentOrder ASC, criterionName ASC")
    private List<CriterionResultJpaEntity> results = new ArrayList<>();
    private Integer automaticScore;
    private Integer finalScore;
    private Integer maxScore;
    private Long durationSeconds;
    private Long timeLimitSeconds;
    private Long overtimeSeconds;
    private UUID expertId;
    @Column(columnDefinition = "text")
    private String expertComment;
    private Instant confirmedAt;

    public void setResults(List<CriterionResultJpaEntity> results) {
        this.results.clear();
        results.forEach(result -> {
            result.setReview(this);
            this.results.add(result);
        });
    }
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
