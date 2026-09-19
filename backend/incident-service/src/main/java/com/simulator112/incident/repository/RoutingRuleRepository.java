package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.RoutingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoutingRuleRepository extends JpaRepository<RoutingRuleEntity, UUID> {

    @Query("""
            SELECT DISTINCT rule
            FROM RoutingRuleEntity rule
            JOIN FETCH rule.variant variant
            JOIN FETCH variant.dispatchService
            LEFT JOIN FETCH variant.conditions
            WHERE rule.classifierEntry.id = :classifierEntryId
            ORDER BY variant.position
            """)
    List<RoutingRuleEntity> findAllForRouting(@Param("classifierEntryId") UUID classifierEntryId);
}
