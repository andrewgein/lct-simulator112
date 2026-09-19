package com.simulator112.incident.model.entity.classifier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "classifier_entries")
public class ClassifierEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ClassifierCategoryEntity category;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "feature_1_code")
    private String feature1Code;

    @Column(name = "feature_1_name", columnDefinition = "text")
    private String feature1Name;

    @Column(name = "feature_2_code")
    private String feature2Code;

    @Column(name = "feature_2_name", columnDefinition = "text")
    private String feature2Name;

    @Column(name = "feature_3_code")
    private String feature3Code;

    @Column(name = "feature_3_name", columnDefinition = "text")
    private String feature3Name;

    @Column(name = "statistical_group", columnDefinition = "text")
    private String statisticalGroup;

    @Column(name = "additional_features", columnDefinition = "text")
    private String additionalFeatures;

    @Column(name = "final_name", nullable = false, columnDefinition = "text")
    private String finalName;

    @Column(name = "ekp_35_name", columnDefinition = "text")
    private String ekp35Name;

    @Column(name = "primary_service_raw", columnDefinition = "text")
    private String primaryServiceRaw;

    @Column(nullable = false)
    private Integer position;

    @ManyToMany
    @JoinTable(
            name = "classifier_entry_primary_services",
            joinColumns = @JoinColumn(name = "classifier_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "dispatch_service_id")
    )
    @BatchSize(size = 100)
    @Builder.Default
    private List<DispatchServiceEntity> primaryServices = new ArrayList<>();

    @OneToMany(mappedBy = "classifierEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoutingRuleEntity> routingRules = new ArrayList<>();

    public void addPrimaryService(DispatchServiceEntity service) {
        primaryServices.add(service);
    }

    public void addRoutingRule(RoutingRuleEntity routingRule) {
        routingRules.add(routingRule);
        routingRule.setClassifierEntry(this);
    }
}
