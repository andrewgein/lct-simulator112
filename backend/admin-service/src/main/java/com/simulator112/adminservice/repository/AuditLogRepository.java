package com.simulator112.adminservice.repository;

import com.simulator112.adminservice.entity.AuditLogEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
    extends JpaRepository<AuditLogEntry, UUID>, JpaSpecificationExecutor<AuditLogEntry> {}
