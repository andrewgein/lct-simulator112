package com.simulator112.profileservice.application.port.out;

import com.simulator112.profileservice.domain.model.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    Optional<UserProfile> findById(UUID userId);

    List<UserProfile> findAll();

    UserProfile save(UserProfile profile);

    void delete(UserProfile profile);
}
