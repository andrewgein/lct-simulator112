package com.simulator112.contextmanager.adapter.out.persistence.repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.Context;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.StageStatus;

public interface ContextRepository extends JpaRepository<Context, UUID> {
    Optional<Context> findFirstByUserIdAndAssignmentIdAndStatusNotInOrderByCreatedAtDesc(
            UUID userId, UUID assignmentId, List<ContextStatus> excludedStatuses);

    List<Context> findByStatusNotInAndUpdatedAtBefore(List<ContextStatus> excludedStatuses, Instant updatedBefore);

    @Query("""
            select distinct context from Context context
            join context.incidentContexts incident
            join incident.stages stage
            where context.targetType = :targetType
              and incident.status = :incidentStatus
              and stage.status = :stageStatus
              and stage.deadlineAt <= :now
            """)
    List<Context> findWithExpiredStages(
            @Param("targetType") IncidentTargetType targetType,
            @Param("incidentStatus") IncidentProgressStatus incidentStatus,
            @Param("stageStatus") StageStatus stageStatus,
            @Param("now") Instant now);
}
