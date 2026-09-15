package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.DialupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DialupRepository extends JpaRepository<DialupEntity, UUID> {
}
