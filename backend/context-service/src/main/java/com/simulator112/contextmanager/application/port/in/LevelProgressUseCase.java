package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.domain.common.LevelProgress;
import com.simulator112.contextmanager.domain.dds.DdsStageSignal;
import java.util.UUID;

public interface LevelProgressUseCase {
    LevelProgress getProgress(UUID contextId);

    LevelProgress applyDdsSignal(UUID contextId, UUID incidentId, DdsStageSignal signal);
}
