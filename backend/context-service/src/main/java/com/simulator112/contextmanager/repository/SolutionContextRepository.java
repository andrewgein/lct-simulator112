package com.simulator112.contextmanager.repository;

import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolutionContextRepository extends JpaRepository<SolutionContextEntity, UUID> {

    Optional<SolutionContextEntity> findTopByContextUuidAndCardIdOrderByVersionDesc(UUID contextId, UUID cardId);

    Optional<SolutionContextEntity> findTopByContextUuidAndDialupIdOrderByCreatedAtDesc(UUID contextId, UUID dialupId);

    List<SolutionContextEntity> findByContextUuidOrderByCreatedAtAsc(UUID contextId);

}
