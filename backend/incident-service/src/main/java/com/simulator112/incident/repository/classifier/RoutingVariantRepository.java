package com.simulator112.incident.repository.classifier;

import com.simulator112.incident.model.entity.classifier.RoutingVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutingVariantRepository extends JpaRepository<RoutingVariantEntity, UUID> {
}
