package com.simulator112.contextmanager.domain.dds;

import java.util.UUID;

public record DdsStageTransition(UUID stageId, UUID successStageId, UUID failureStageId) {
}
