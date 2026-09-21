package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContextStore {
    Optional<TrainingContext> findById(UUID id);

    TrainingContext save(TrainingContext context);

    List<TrainingContext> findWithExpiredStages(IncidentTargetType targetType,
                                                IncidentProgressStatus incidentStatus,
                                                StageStatus stageStatus,
                                                Instant now);
}
