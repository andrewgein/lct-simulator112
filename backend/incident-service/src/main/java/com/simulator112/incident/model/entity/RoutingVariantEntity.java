package com.simulator112.incident.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routing_variants")
public class RoutingVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispatch_service_id", nullable = false)
    private DispatchServiceEntity dispatchService;

    @Column(name = "routing_target", nullable = false, columnDefinition = "text")
    private String routingTarget;

    @Column(name = "source_column", nullable = false, unique = true, length = 3)
    private String sourceColumn;

    @Column(name = "header_level_1", columnDefinition = "text")
    private String headerLevel1;

    @Column(name = "header_level_2", columnDefinition = "text")
    private String headerLevel2;

    @Column(name = "header_level_3", columnDefinition = "text")
    private String headerLevel3;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, unique = true)
    private Integer position;

    @OneToMany(mappedBy = "routingVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<RoutingVariantConditionEntity> conditions = new ArrayList<>();

    public void addCondition(RoutingVariantConditionEntity condition) {
        conditions.add(condition);
        condition.setRoutingVariant(this);
    }
}
