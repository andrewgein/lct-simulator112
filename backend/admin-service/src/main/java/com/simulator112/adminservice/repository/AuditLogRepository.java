package com.simulator112.adminservice.repository;

import com.simulator112.adminservice.entity.AuditLogEntry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

  @Query(
      """
      select a from AuditLogEntry a
      where (:userId is null or a.actorUserId = :userId)
        and (:action is null or a.action = :action)
        and (:dateFrom is null or a.occurredAt >= :dateFrom)
        and (:dateTo is null or a.occurredAt <= :dateTo)
      order by a.occurredAt desc
      """)
  Page<AuditLogEntry> search(
      @Param("userId") UUID userId,
      @Param("action") String action,
      @Param("dateFrom") Instant dateFrom,
      @Param("dateTo") Instant dateTo,
      Pageable pageable);
}
