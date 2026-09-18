package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.RoutingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutingRuleRepository extends JpaRepository<RoutingRuleEntity, UUID> {
}
