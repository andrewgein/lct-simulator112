package com.simulator112.incident.domain.dds;

import java.util.UUID;

public record DdsStageTransition(
        UUID stageId,
        UUID successStageId,
        UUID failureStageId) {
}
