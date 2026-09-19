package com.simulator112.incident.repository.classifier;

import com.simulator112.incident.model.entity.classifier.DispatchServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DispatchServiceRepository extends JpaRepository<DispatchServiceEntity, UUID> {

    Optional<DispatchServiceEntity> findByCode(String code);
}
