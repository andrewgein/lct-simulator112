package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.AdditionalInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdditionalInfoRepository extends JpaRepository<AdditionalInfoEntity, UUID> {
}
