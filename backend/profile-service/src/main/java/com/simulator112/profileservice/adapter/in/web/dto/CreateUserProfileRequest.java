package com.simulator112.profileservice.adapter.in.web.dto;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import jakarta.validation.constraints.NotBlank;

public record CreateUserProfileRequest(
        @NotBlank String name,
        @NotBlank String surname,
        String patronymic,
        TrainingTrack trainingTrack,
        DdsService ddsService) {

    public ProfessionalProfile toProfessionalProfile() {
        if (trainingTrack == null && ddsService == null) {
            return null;
        }
        return new ProfessionalProfile(trainingTrack, ddsService);
    }
}
