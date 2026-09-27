package com.simulator112.adminservice.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntry {

  private UUID id;
  private Instant occurredAt;
  private UUID actorUserId;
  private String actorEmail;
  private String actorRole;
  private String action;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
  private Map<String, Object> details;
  private String sourceService;
}
