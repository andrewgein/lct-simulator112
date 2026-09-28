package com.simulator112.adminservice.adapter.out.persistence.repository;

import com.simulator112.adminservice.adapter.out.persistence.entity.AuditLogJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataAuditLogRepository
    extends JpaRepository<AuditLogJpaEntity, UUID>, JpaSpecificationExecutor<AuditLogJpaEntity> {}
