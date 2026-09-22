package com.simulator112.incident.adapter.out.persistence.repository;

import com.simulator112.incident.adapter.out.persistence.entity.common.LevelJpaEntity;
import com.simulator112.incident.domain.common.IncidentTargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLevelRepository extends JpaRepository<LevelJpaEntity, UUID> {
    List<LevelJpaEntity> findAllByTargetType(IncidentTargetType targetType);
}
