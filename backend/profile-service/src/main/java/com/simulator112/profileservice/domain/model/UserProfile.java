package com.simulator112.profileservice.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        String name,
        String surname,
        String patronymic,
        ProfessionalProfile professionalProfile,
        Instant updatedAt) {

    public static UserProfile create(UUID userId, String name, String surname) {
        return create(userId, name, surname, null, null);
    }

    public static UserProfile create(
            UUID userId, String name, String surname, ProfessionalProfile professionalProfile) {
        return create(userId, name, surname, null, professionalProfile);
    }

    public static UserProfile create(
            UUID userId,
            String name,
            String surname,
            String patronymic,
            ProfessionalProfile professionalProfile) {
        return new UserProfile(userId, name, surname, patronymic, professionalProfile, null);
    }

    public UserProfile updatePersonalData(String newName, String newSurname, String newPatronymic) {
        return new UserProfile(
                userId,
                newName != null ? newName : name,
                newSurname != null ? newSurname : surname,
                newPatronymic != null ? newPatronymic : patronymic,
                professionalProfile,
                updatedAt);
    }

    public UserProfile assignProfessionalProfile(ProfessionalProfile profile) {
        return new UserProfile(userId, name, surname, patronymic, profile, updatedAt);
    }
}
