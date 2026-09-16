package com.simulator112.userservice.mapper;

import com.simulator112.userservice.entity.UserProfile;
import com.simulator112.userservice.model.request.UserProfileRequest;
import com.simulator112.userservice.model.response.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
  public UserProfile toEntity(UserProfileRequest request) {

    UserProfile profile = new UserProfile();

    profile.setName(request.getName());
    profile.setSurname(request.getSurname());

    return profile;
  }

  public UserProfileResponse toUserProfileView(UserProfile profile) {
    return UserProfileResponse.builder()
        .userId(profile.getUserId())
        .name(profile.getName())
        .surname(profile.getSurname())
        .updatedAt(profile.getUpdatedAt())
        .build();
  }
}
