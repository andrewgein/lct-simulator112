package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.CreateUserProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UpdateUserProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UserProfileResponse;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileUseCase userProfiles;

    @GetMapping
    public UserProfileResponse getProfile(@RequestHeader("X-User-Id") UUID userId) {
        return UserProfileWebMapper.toResponse(userProfiles.getProfile(userId));
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUserProfile(@PathVariable UUID userId) {
        return UserProfileWebMapper.toResponse(userProfiles.getProfile(userId));
    }

    @PostMapping
    public UserProfileResponse createProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateUserProfileRequest request) {
        ProfessionalProfile professionalProfile = request.toProfessionalProfile();
        if ("STUDENT".equals(role) && professionalProfile == null) {
            throw new InvalidProfessionalProfileException("Направление обучения обязательно");
        }
        return UserProfileWebMapper.toResponse(
                userProfiles.createProfile(
                        userId,
                        request.name(),
                        request.surname(),
                        request.patronymic(),
                        professionalProfile));
    }

    @PatchMapping("/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable UUID userId, @RequestBody UpdateUserProfileRequest request) {
        return update(userId, request, false);
    }

    @PatchMapping
    public UserProfileResponse updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody UpdateUserProfileRequest request) {
        return update(userId, request, "SUPERVISOR".equals(role));
    }

    @DeleteMapping("/{userId}")
    public void deleteUserProfile(@PathVariable UUID userId) {
        userProfiles.deleteProfile(userId);
    }

    private UserProfileResponse update(
            UUID userId, UpdateUserProfileRequest request, boolean updateProfessionalProfile) {
        ProfessionalProfile professionalProfile = updateProfessionalProfile
                ? request.toProfessionalProfile()
                : userProfiles.getProfile(userId).professionalProfile();
        return UserProfileWebMapper.toResponse(
                userProfiles.updateProfile(
                        userId,
                        request.name(),
                        request.surname(),
                        request.patronymic(),
                        professionalProfile));
    }
}
