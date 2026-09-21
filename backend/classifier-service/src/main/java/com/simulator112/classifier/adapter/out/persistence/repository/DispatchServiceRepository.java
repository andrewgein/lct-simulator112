package com.simulator112.classifier.adapter.out.persistence.repository;

import com.simulator112.classifier.adapter.out.persistence.entity.DispatchServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DispatchServiceRepository extends JpaRepository<DispatchServiceEntity, UUID> {

    Optional<DispatchServiceEntity> findByCode(String code);
}
