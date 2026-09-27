package com.simulator112.adminservice.adapter.in.web.dto;

import com.simulator112.adminservice.domain.model.AuditLogEntry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    Instant occurredAt,
    UUID actorUserId,
    String actorEmail,
    String actorRole,
    String action,
    String resourceType,
    String resourceId,
    String ipAddress,
    Map<String, Object> details,
    String sourceService) {

  public static AuditLogResponse from(AuditLogEntry entry) {
    return new AuditLogResponse(
        entry.getId(),
        entry.getOccurredAt(),
        entry.getActorUserId(),
        entry.getActorEmail(),
        entry.getActorRole(),
        entry.getAction(),
        entry.getResourceType(),
        entry.getResourceId(),
        entry.getIpAddress(),
        entry.getDetails(),
        entry.getSourceService());
  }
}
