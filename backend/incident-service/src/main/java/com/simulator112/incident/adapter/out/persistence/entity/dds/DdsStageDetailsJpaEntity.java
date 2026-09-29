package com.simulator112.incident.adapter.out.persistence.entity.dds;

import com.simulator112.incident.adapter.out.persistence.entity.common.IncidentStageJpaEntity;
import com.simulator112.incident.domain.dds.DdsStageType;
import com.simulator112.incident.domain.dds.DdsCompletionTrigger;
import com.simulator112.incident.domain.common.IncidentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dds_stage_details")
public class DdsStageDetailsJpaEntity {
    @Id
    @Column(name = "stage_id")
    private UUID stageId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private IncidentStageJpaEntity stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false, length = 50)
    private DdsStageType type;

    @Column(name = "time_limit_seconds", nullable = false)
    private int timeLimitSeconds;

    @Column(name = "expected_comment", columnDefinition = "text")
    private String expectedComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_status", length = 50)
    private IncidentStatus actualStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dds_stage_completion_triggers", joinColumns = @JoinColumn(name = "stage_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_trigger", nullable = false, length = 20)
    private java.util.List<DdsCompletionTrigger> completionTriggers = new java.util.ArrayList<>();
}
