package com.simulator112.profileservice.adapter.in.web.dto;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import jakarta.validation.constraints.NotNull;

public record AssignProfessionalProfileRequest(
        @NotNull TrainingTrack trainingTrack,
        DdsService ddsService) {

    public ProfessionalProfile toDomain() {
        return new ProfessionalProfile(trainingTrack, ddsService);
    }
}
