package com.simulator112.adminservice.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the payload published by business services for semantically meaningful admin actions
 * (e.g. auth-service's role-change), as opposed to the generic HTTP-level events from the gateway. */
@Data
@NoArgsConstructor
public class DomainAuditEventMessage {
  private String actorUserId;
  private String actorEmail;
  private String actorRole;
  private String action;
  private String resourceType;
  private String resourceId;
  private Map<String, Object> details;
  private String sourceService;
  private Instant timestamp;
}
