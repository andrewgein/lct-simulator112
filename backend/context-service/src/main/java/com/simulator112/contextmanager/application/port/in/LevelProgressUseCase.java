package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.domain.common.LevelProgress;
import com.simulator112.contextmanager.domain.common.ReactionStatus;
import java.util.UUID;

public interface LevelProgressUseCase {
    LevelProgress getProgress(UUID contextId);

    LevelProgress saveDdsComment(UUID contextId, UUID incidentId, UUID stageId, String comment);

    LevelProgress applyReactionStatus(UUID contextId, UUID incidentId, String serviceCode,
                                      ReactionStatus status, String comment);
}
