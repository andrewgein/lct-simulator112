package com.simulator112.auth.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestedEvent {
  private UUID userId;
  private String email;
  private String resetLink;
}
