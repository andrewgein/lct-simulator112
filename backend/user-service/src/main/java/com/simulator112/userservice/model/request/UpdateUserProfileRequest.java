package com.simulator112.userservice.model.request;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {

  private String name;

  private String surname;
}
