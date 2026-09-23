package com.simulator112.profileservice.adapter.in.web.dto;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;

public record UpdateUserProfileRequest(
        String name,
        String surname,
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
