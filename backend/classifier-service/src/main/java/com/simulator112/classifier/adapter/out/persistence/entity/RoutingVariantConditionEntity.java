package com.simulator112.classifier.adapter.out.persistence.entity;

import com.simulator112.classifier.domain.model.RoutingConditionOperator;
import jakarta.persistence.*;
import lombok.*;

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
