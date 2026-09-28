package com.simulator112.adminservice.adapter.in.kafka.dto;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

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
