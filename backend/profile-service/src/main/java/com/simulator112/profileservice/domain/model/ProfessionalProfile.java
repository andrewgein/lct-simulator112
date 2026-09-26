package com.simulator112.profileservice.domain.model;

import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;

public record ProfessionalProfile(TrainingTrack trainingTrack, String ddsService) {

    public ProfessionalProfile {
        if (trainingTrack == null) {
            throw new InvalidProfessionalProfileException("Направление обучения обязательно");
        }
        if (trainingTrack == TrainingTrack.SYSTEM_112 && ddsService != null) {
            throw new InvalidProfessionalProfileException(
                    "Для направления SYSTEM_112 служба ДДС не указывается");
        }
        if (trainingTrack == TrainingTrack.DDS && (ddsService == null || ddsService.isBlank())) {
            throw new InvalidProfessionalProfileException(
                    "Для направления DDS требуется служба ДДС");
        }
    }
}
