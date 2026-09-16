package com.simulator112.userservice.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileRequest {
  @NotBlank private String name;

  @NotBlank private String surname;
}
