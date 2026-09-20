package com.simulator112.incident.adapter.out.persistence.entity.dds;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DdsStageTransitionEmbeddable {
    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "success_stage_id")
    private UUID successStageId;

    @Column(name = "failure_stage_id")
    private UUID failureStageId;
}
