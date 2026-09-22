package com.simulator112.profileservice.adapter.in.web.dto;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserProfileRequest(
        @NotBlank String name,
        @NotBlank String surname,
        @NotNull TrainingTrack trainingTrack,
        DdsService ddsService) {

    public ProfessionalProfile toProfessionalProfile() {
        return new ProfessionalProfile(trainingTrack, ddsService);
    }
}
