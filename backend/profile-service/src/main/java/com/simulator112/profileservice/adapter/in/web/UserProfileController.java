package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.CreateUserProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UpdateUserProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UserProfileResponse;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/all")
    public List<UserProfileResponse> getAllUserProfiles() {
        return userProfiles.getAllProfiles().stream().map(UserProfileWebMapper::toResponse).toList();
    }

    @PostMapping
    public UserProfileResponse createProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateUserProfileRequest request) {
        return UserProfileWebMapper.toResponse(
                userProfiles.createProfile(userId, request.name(), request.surname()));
    }

    @PatchMapping("/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable UUID userId, @RequestBody UpdateUserProfileRequest request) {
        return update(userId, request);
    }

    @PatchMapping
    public UserProfileResponse updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdateUserProfileRequest request) {
        return update(userId, request);
    }

    @DeleteMapping("/{userId}")
    public void deleteUserProfile(@PathVariable UUID userId) {
        userProfiles.deleteProfile(userId);
    }

    private UserProfileResponse update(UUID userId, UpdateUserProfileRequest request) {
        return UserProfileWebMapper.toResponse(
                userProfiles.updateProfile(userId, request.name(), request.surname()));
    }
}
