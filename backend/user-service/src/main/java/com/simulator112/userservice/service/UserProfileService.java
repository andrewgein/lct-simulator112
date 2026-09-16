package com.simulator112.userservice.service;

import com.simulator112.userservice.entity.UserProfile;
import com.simulator112.userservice.exception.ProfileNotFoundException;
import com.simulator112.userservice.mapper.UserProfileMapper;
import com.simulator112.userservice.model.request.UpdateUserProfileRequest;
import com.simulator112.userservice.model.request.UserProfileRequest;
import com.simulator112.userservice.model.response.UserProfileResponse;
import com.simulator112.userservice.repository.UserProfileRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository repository;
  private final UserProfileMapper mapper;
  private final EntityManager entityManager;

  private UserProfile getProfileByIdOrThrow(UUID userId) {
    return repository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
  }

  @Transactional(readOnly = true)
  public List<UserProfileResponse> getAllUsers() {
    return repository.findAll().stream().map(mapper::toUserProfileView).toList();
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(UUID userId) {
    UserProfile profile = getProfileByIdOrThrow(userId);
    return mapper.toUserProfileView(profile);
  }

  private UserProfileResponse updateProfile(UserProfile profile, UpdateUserProfileRequest request) {
    if (request.getName() != null) {
      profile.setName(request.getName());
    }

    if (request.getSurname() != null) {
      profile.setSurname(request.getSurname());
    }

    UserProfile updatedProfile = repository.save(profile);

    log.info("Обновленны данные сотрудника с id {}", updatedProfile.getUserId());

    return mapper.toUserProfileView(updatedProfile);
  }

  @Transactional
  public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
    UserProfile profile = getProfileByIdOrThrow(userId);
    return updateProfile(profile, request);
  }

  @Transactional
  public UserProfileResponse createProfile(UUID userId, UserProfileRequest request) {
    UserProfile profile = mapper.toEntity(request);
    profile.setUserId(userId);

    entityManager.persist(profile);

    log.info("Создан сотрудник с id {}", profile.getUserId());

    return mapper.toUserProfileView(profile);
  }

  @Transactional
  public void deleteProfile(UUID userId) {
    UserProfile profile = getProfileByIdOrThrow(userId);

    repository.delete(profile);
    log.info("Удален сотрудник с id {}", profile.getUserId());
  }
}
