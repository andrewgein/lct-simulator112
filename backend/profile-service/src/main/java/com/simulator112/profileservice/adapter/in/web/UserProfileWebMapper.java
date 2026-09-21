package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.UserProfileResponse;
import com.simulator112.profileservice.domain.model.UserProfile;

final class UserProfileWebMapper {

    private UserProfileWebMapper() {
    }

    static UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.userId(),
                null,
                profile.name(),
                profile.surname(),
                profile.professionalProfile() == null
                        ? null
                        : profile.professionalProfile().trainingTrack(),
                profile.professionalProfile() == null
                        ? null
                        : profile.professionalProfile().ddsService(),
                profile.updatedAt());
    }
}
