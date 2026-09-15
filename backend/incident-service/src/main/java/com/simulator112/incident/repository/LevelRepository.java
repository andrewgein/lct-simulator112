package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.LevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LevelRepository extends JpaRepository<LevelEntity, UUID> {
}
