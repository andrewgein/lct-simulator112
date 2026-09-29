package com.simulator112.contextmanager.adapter.out.persistence.entity.dds;

import com.simulator112.contextmanager.adapter.out.persistence.entity.common.StageContextEntity;

import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger;
import com.simulator112.contextmanager.domain.common.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dds_stage_contexts")
@Getter
@Setter
public class DdsStageContextEntity {
    @Id
    private UUID stageContextId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_context_id")
    private StageContextEntity stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "dds_stage_type", nullable = false)
    private DdsStageType type;

    private Integer timeLimitSeconds;

    @Column(name = "fail_on_timeout", nullable = false)
    private boolean failOnTimeout;

    @Column(columnDefinition = "text")
    private String expectedComment;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_status", length = 50)
    private IncidentStatus actualStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dds_stage_completion_triggers", joinColumns = @JoinColumn(name = "stage_context_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_trigger", nullable = false, length = 20)
    private java.util.List<DdsCompletionTrigger> completionTriggers = new java.util.ArrayList<>();
}
