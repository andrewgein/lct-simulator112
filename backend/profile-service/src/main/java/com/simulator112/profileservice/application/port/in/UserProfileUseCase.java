package com.simulator112.profileservice.application.port.in;

import com.simulator112.profileservice.domain.model.UserProfile;

import java.util.List;
import java.util.UUID;

public interface UserProfileUseCase {

    UserProfile getProfile(UUID userId);

    List<UserProfile> getAllProfiles();

    UserProfile createProfile(UUID userId, String name, String surname);

    UserProfile updateProfile(UUID userId, String name, String surname);

    void deleteProfile(UUID userId);
}
