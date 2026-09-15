package com.simulator112.review_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Audited;

import java.util.UUID;

@Data
@Entity
@Table(name = "criterion_results")
public class CriterionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", insertable = false, updatable = false)
    private UUID reviewId;

    private String incidentId;
    private Integer incidentOrder;
    private String criterionName;
    private Integer score;
    private Integer maxScore;
    private String feedback;
}
