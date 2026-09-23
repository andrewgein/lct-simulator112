package com.simulator112.review_service.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "criterion_results")
@Getter
@Setter
public class CriterionResultJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewJpaEntity review;
    private String incidentId;
    private Integer incidentOrder;
    private String criterionName;
    private Integer score;
    private Integer maxScore;
    private String feedback;
}
