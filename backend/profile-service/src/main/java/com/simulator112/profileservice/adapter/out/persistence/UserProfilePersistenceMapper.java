package com.simulator112.profileservice.adapter.out.persistence;

import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.UserProfile;

final class UserProfilePersistenceMapper {

    private UserProfilePersistenceMapper() {
    }

    static UserProfile toDomain(UserProfileJpaEntity entity) {
        ProfessionalProfile professionalProfile = entity.getTrainingTrack() == null
                ? null
                : new ProfessionalProfile(entity.getTrainingTrack(), entity.getDdsService());
        return new UserProfile(
                entity.getUserId(),
                entity.getName(),
                entity.getSurname(),
                entity.getPatronymic(),
                professionalProfile,
                entity.getUpdatedAt());
    }

    static UserProfileJpaEntity newEntity(UserProfile profile) {
        UserProfileJpaEntity entity = new UserProfileJpaEntity();
        entity.setUserId(profile.userId());
        updateEntity(entity, profile);
        return entity;
    }

    static void updateEntity(UserProfileJpaEntity entity, UserProfile profile) {
        entity.setName(profile.name());
        entity.setSurname(profile.surname());
        entity.setPatronymic(profile.patronymic());
        entity.setTrainingTrack(
                profile.professionalProfile() == null
                        ? null
                        : profile.professionalProfile().trainingTrack());
        entity.setDdsService(
                profile.professionalProfile() == null
                        ? null
                        : profile.professionalProfile().ddsService());
    }
}
