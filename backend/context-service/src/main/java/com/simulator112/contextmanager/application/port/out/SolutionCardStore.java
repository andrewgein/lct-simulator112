package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolutionCardStore {
    SolutionCardRevision save(SolutionCardRevision revision);

    Optional<SolutionCardRevision> findLatestByCard(UUID contextId, UUID cardId);

    Optional<SolutionCardRevision> findLatestByCall(UUID contextId, UUID callId);

    List<SolutionCardRevision> findAll(UUID contextId);
}
