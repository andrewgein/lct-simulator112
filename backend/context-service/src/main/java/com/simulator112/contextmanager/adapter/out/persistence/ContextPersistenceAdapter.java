package com.simulator112.contextmanager.adapter.out.persistence;

import com.simulator112.contextmanager.adapter.out.persistence.repository.ContextRepository;
import com.simulator112.contextmanager.adapter.out.persistence.repository.SolutionContextRepository;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.SolutionCardStore;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContextPersistenceAdapter implements ContextStore, SolutionCardStore {
    private final ContextRepository contextRepository;
    private final SolutionContextRepository solutionContextRepository;

    @Override
    public Optional<TrainingContext> findById(UUID id) {
        return contextRepository.findById(id).map(ContextPersistenceMapper::toDomain);
    }

    @Override
    public Optional<TrainingContext> findActive(UUID userId, UUID assignmentId) {
        return contextRepository.findFirstByUserIdAndAssignmentIdAndStatusNotInOrderByCreatedAtDesc(
                userId, assignmentId, List.of(ContextStatus.IN_REVIEW, ContextStatus.DONE))
                .map(ContextPersistenceMapper::toDomain);
    }

    @Override
    public TrainingContext save(TrainingContext context) {
        return ContextPersistenceMapper.toDomain(contextRepository.save(ContextPersistenceMapper.toEntity(context)));
    }

    @Override
    public List<TrainingContext> findWithExpiredStages(IncidentTargetType targetType,
                                                       IncidentProgressStatus incidentStatus,
                                                       StageStatus stageStatus,
                                                       Instant now) {
        return contextRepository.findWithExpiredStages(targetType, incidentStatus, stageStatus, now).stream()
                .map(ContextPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<TrainingContext> findAbandoned(Instant updatedBefore) {
        return contextRepository.findByStatusNotInAndUpdatedAtBefore(
                List.of(ContextStatus.IN_REVIEW, ContextStatus.DONE), updatedBefore).stream()
                .map(ContextPersistenceMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        contextRepository.deleteById(id);
    }

    @Override
    public SolutionCardRevision save(SolutionCardRevision revision) {
        var entity = ContextPersistenceMapper.toEntity(revision);
        entity.setContext(contextRepository.getReferenceById(revision.getContextId()));
        return ContextPersistenceMapper.toDomain(solutionContextRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<SolutionCardRevision> findLatestByCard(UUID contextId, UUID cardId) {
        return solutionContextRepository.findTopByContextUuidAndCardIdOrderByVersionDesc(contextId, cardId)
                .map(ContextPersistenceMapper::toDomain);
    }

    @Override
    public Optional<SolutionCardRevision> findLatestByCall(UUID contextId, UUID callId) {
        return solutionContextRepository.findTopByContextUuidAndCallIdOrderByCreatedAtDesc(contextId, callId)
                .map(ContextPersistenceMapper::toDomain);
    }

    @Override
    public List<SolutionCardRevision> findAll(UUID contextId) {
        return solutionContextRepository.findByContextUuidOrderByCreatedAtAsc(contextId).stream()
                .map(ContextPersistenceMapper::toDomain).toList();
    }
}
