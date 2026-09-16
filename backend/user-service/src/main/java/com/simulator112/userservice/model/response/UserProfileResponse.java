package com.simulator112.userservice.model.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

  private UUID userId;

  private UUID authId;

  private String name;

  private String surname;

  private Instant updatedAt;
}
