package com.simulator112.classifier.adapter.out.persistence.entity;

import com.simulator112.classifier.domain.model.RoutingResultKind;
import jakarta.persistence.*;
import lombok.*;

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
