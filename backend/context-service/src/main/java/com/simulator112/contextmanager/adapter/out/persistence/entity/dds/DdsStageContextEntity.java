package com.simulator112.contextmanager.adapter.out.persistence.entity.dds;

import com.simulator112.contextmanager.adapter.out.persistence.entity.common.StageContextEntity;

import com.simulator112.contextmanager.domain.dds.DdsStageType;
import jakarta.persistence.Column;
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

    @Column(columnDefinition = "text")
    private String expectedComment;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "actual_status", length = 50)
    private String actualStatus;
}
