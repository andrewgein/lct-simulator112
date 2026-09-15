package com.simulator112.incident.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simulator112.incident.model.entity.IncidentEntity;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
}
