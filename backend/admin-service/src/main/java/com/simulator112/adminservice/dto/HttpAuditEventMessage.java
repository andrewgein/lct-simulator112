package com.simulator112.adminservice.dto;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the payload published by api-gateway's AuditLoggingFilter for every mutating admin request. */
@Data
@NoArgsConstructor
public class HttpAuditEventMessage {
  private String userId;
  private String email;
  private String role;
  private String method;
  private String path;
  private int status;
  private String ip;
  private Instant timestamp;
}
