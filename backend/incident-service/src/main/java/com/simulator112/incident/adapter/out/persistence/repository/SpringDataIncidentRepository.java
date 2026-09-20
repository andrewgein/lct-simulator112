package com.simulator112.incident.adapter.out.persistence.repository;

import com.simulator112.incident.adapter.out.persistence.entity.common.IncidentJpaEntity;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataIncidentRepository extends JpaRepository<IncidentJpaEntity, UUID> {
    List<IncidentJpaEntity> findAllByTargetTypeAndDifficulty(
            IncidentTargetType targetType, Difficulty difficulty);
}
