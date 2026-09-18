package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.RoutingVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutingVariantRepository extends JpaRepository<RoutingVariantEntity, UUID> {
}
