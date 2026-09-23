package com.simulator112.profileservice.application.service;

import com.simulator112.profileservice.application.port.in.AssignProfessionalProfileUseCase;
import com.simulator112.profileservice.application.port.in.GetProfessionalProfileUseCase;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import com.simulator112.profileservice.application.port.out.UserProfileRepository;
import com.simulator112.profileservice.domain.exception.ProfessionalProfileNotAssignedException;
import com.simulator112.profileservice.domain.exception.ProfileNotFoundException;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileApplicationService
        implements UserProfileUseCase, GetProfessionalProfileUseCase, AssignProfessionalProfileUseCase {

    private final UserProfileRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> getAllProfiles() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return getProfileOrThrow(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalProfile getProfessionalProfile(UUID userId) {
        UserProfile profile = getProfileOrThrow(userId);
        if (profile.professionalProfile() == null) {
            throw new ProfessionalProfileNotAssignedException(userId);
        }
        return profile.professionalProfile();
    }

    @Override
    @Transactional
    public UserProfile assignProfessionalProfile(
            UUID userId, ProfessionalProfile professionalProfile) {
        UserProfile updated = repository.save(
                getProfileOrThrow(userId).assignProfessionalProfile(professionalProfile));
        log.info(
                "Пользователю с id {} назначено направление обучения {}",
                userId,
                professionalProfile.trainingTrack());
        return updated;
    }

    @Override
    @Transactional
    public UserProfile createProfile(
            UUID userId, String name, String surname, String patronymic, ProfessionalProfile professionalProfile) {
        UserProfile profile = repository.save(
                UserProfile.create(userId, name, surname, patronymic, professionalProfile));
        log.info(
                "Создан сотрудник с id {} и направлением обучения {}",
                userId,
                professionalProfile == null ? null : professionalProfile.trainingTrack());
        return profile;
    }

    @Override
    @Transactional
    public UserProfile updateProfile(
            UUID userId,
            String name,
            String surname,
            String patronymic,
            ProfessionalProfile professionalProfile) {
        UserProfile updated = repository.save(
                getProfileOrThrow(userId)
                        .updatePersonalData(name, surname, patronymic)
                        .assignProfessionalProfile(professionalProfile));
        log.info("Обновлены данные сотрудника с id {}", userId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        UserProfile profile = getProfileOrThrow(userId);
        repository.delete(profile);
        log.info("Удалён сотрудник с id {}", userId);
    }

    private UserProfile getProfileOrThrow(UUID userId) {
        return repository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
