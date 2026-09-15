package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.StageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StageRepository extends JpaRepository<StageEntity, UUID> {
}
