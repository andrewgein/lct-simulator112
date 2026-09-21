package com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DdsStageTransitionSnapshot {
    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "success_stage_id")
    private UUID successStageId;

    @Column(name = "failure_stage_id")
    private UUID failureStageId;
}
