package com.simulator112.auth.adapter.out.kafka.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published to Kafka topic "audit.domain.events", consumed by admin-service's audit log. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleChangedEvent {
  private String actorUserId;
  private String actorEmail;
  private String actorRole;
  private String action;
  private String resourceType;
  private String resourceId;
  private Map<String, Object> details;
  private String sourceService;
  private Instant timestamp;

  public static RoleChangedEvent of(
      UUID actorUserId, String actorEmail, String actorRole, UUID targetUserId, String oldRole, String newRole) {
    return new RoleChangedEvent(
        actorUserId != null ? actorUserId.toString() : null,
        actorEmail,
        actorRole,
        "ROLE_CHANGED",
        "USER",
        targetUserId.toString(),
        Map.of("oldRole", oldRole, "newRole", newRole),
        "auth-service",
        Instant.now());
  }
}
