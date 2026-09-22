package com.simulator112.profileservice.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        String name,
        String surname,
        ProfessionalProfile professionalProfile,
        Instant updatedAt) {

    public static UserProfile create(UUID userId, String name, String surname) {
        return create(userId, name, surname, null);
    }

    public static UserProfile create(
            UUID userId, String name, String surname, ProfessionalProfile professionalProfile) {
        return new UserProfile(userId, name, surname, professionalProfile, null);
    }

    public UserProfile updatePersonalData(String newName, String newSurname) {
        return new UserProfile(
                userId,
                newName != null ? newName : name,
                newSurname != null ? newSurname : surname,
                professionalProfile,
                updatedAt);
    }

    public UserProfile assignProfessionalProfile(ProfessionalProfile profile) {
        return new UserProfile(userId, name, surname, profile, updatedAt);
    }
}
