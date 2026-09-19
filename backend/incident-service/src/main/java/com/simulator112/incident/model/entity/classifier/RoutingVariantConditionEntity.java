package com.simulator112.incident.model.entity.classifier;

import com.simulator112.incident.model.enums.classifier.RoutingConditionOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routing_variant_conditions")
public class RoutingVariantConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routing_variant_id", nullable = false)
    private RoutingVariantEntity routingVariant;

    @Column(name = "fact_code", nullable = false)
    private String factCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutingConditionOperator operator;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(nullable = false)
    private Integer position;
}
