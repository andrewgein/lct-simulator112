package com.simulator112.incident.model.entity;

import com.simulator112.incident.model.enums.RoutingResultKind;
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
@Table(name = "routing_rules")
public class RoutingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classifier_entry_id", nullable = false)
    private ClassifierEntryEntity classifierEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routing_variant_id", nullable = false)
    private RoutingVariantEntity variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_kind", nullable = false)
    private RoutingResultKind resultKind;

    @Column(name = "target_type_name", columnDefinition = "text")
    private String targetTypeName;

    @Column(name = "raw_value", nullable = false, columnDefinition = "text")
    private String rawValue;
}
