package com.simulator112.api_gateway.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published to Kafka topic "audit.http.requests" for every mutating request to a
 * RoleAuthorization-protected route. Consumed by admin-service's audit log. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpAuditEvent {
  private String userId;
  private String email;
  private String role;
  private String method;
  private String path;
  private int status;
  private String ip;
  private Instant timestamp;
}
