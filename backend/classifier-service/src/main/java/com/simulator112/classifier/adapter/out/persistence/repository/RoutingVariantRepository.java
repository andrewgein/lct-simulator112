package com.simulator112.classifier.adapter.out.persistence.repository;

import com.simulator112.classifier.adapter.out.persistence.entity.RoutingVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutingVariantRepository extends JpaRepository<RoutingVariantEntity, UUID> {
}
