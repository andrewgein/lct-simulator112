package com.simulator112.incident.adapter.out.persistence.entity.system112;

import com.simulator112.incident.adapter.out.persistence.entity.common.IncidentStageJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system112_stage_details")
public class System112StageDetailsJpaEntity {
    @Id
    @Column(name = "stage_id")
    private UUID stageId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private IncidentStageJpaEntity stage;

    @ElementCollection
    @CollectionTable(name = "system112_stage_classifier_codes", joinColumns = @JoinColumn(name = "stage_id"))
    @OrderColumn(name = "position")
    @Column(name = "classifier_code", nullable = false, length = 50)
    private List<String> classifierCodes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "system112_stage_expected_routing_facts", joinColumns = @JoinColumn(name = "stage_id"))
    @MapKeyColumn(name = "fact_code")
    @Column(name = "expected_value", nullable = false)
    private Map<String, String> expectedRoutingFacts = new LinkedHashMap<>();

    @Column(name = "victim_count", nullable = false)
    private int victimCount;
}
