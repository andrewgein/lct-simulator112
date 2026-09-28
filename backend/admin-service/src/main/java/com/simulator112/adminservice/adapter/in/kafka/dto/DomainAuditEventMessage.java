package com.simulator112.adminservice.adapter.in.kafka.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

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
