package com.simulator112.contextmanager.domain.dds;

import com.simulator112.contextmanager.domain.common.StageStatus;
import java.time.Instant;
import java.util.UUID;

public record DdsStageProgress(UUID stageId, DdsStageType type, StageStatus status,
                               Instant startedAt, Instant deadline, String comment, boolean completedCall) {
}
