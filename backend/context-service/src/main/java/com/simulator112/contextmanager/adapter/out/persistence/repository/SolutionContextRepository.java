package com.simulator112.contextmanager.adapter.out.persistence.repository;

import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.SolutionContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolutionContextRepository extends JpaRepository<SolutionContextEntity, UUID> {

    Optional<SolutionContextEntity> findTopByContextUuidAndCardIdOrderByVersionDesc(UUID contextId, UUID cardId);

    Optional<SolutionContextEntity> findTopByContextUuidAndCallIdOrderByCreatedAtDesc(UUID contextId, UUID callId);

    List<SolutionContextEntity> findByContextUuidOrderByCreatedAtAsc(UUID contextId);

}
