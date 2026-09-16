package com.simulator112.userservice.controller;

import com.simulator112.userservice.model.request.UpdateUserProfileRequest;
import com.simulator112.userservice.model.request.UserProfileRequest;
import com.simulator112.userservice.model.response.UserProfileResponse;
import com.simulator112.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserProfileService userProfileService;

  @GetMapping
  public UserProfileResponse getProfile(@RequestHeader("X-User-Id") UUID userId) {
    return userProfileService.getProfile(userId);
  }

  @GetMapping("/{userId}")
  public UserProfileResponse getUserProfile(@PathVariable UUID userId) {
    return userProfileService.getProfile(userId);
  }

  @GetMapping("/all")
  public List<UserProfileResponse> getAllUserProfiles() {
    return userProfileService.getAllUsers();
  }

  @PostMapping
  public UserProfileResponse createProfile(
      @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody UserProfileRequest request) {
    return userProfileService.createProfile(userId, request);
  }

  @PatchMapping("/{userId}")
  public UserProfileResponse updateUserProfile(
      @PathVariable UUID userId, @RequestBody UpdateUserProfileRequest request) {
    return userProfileService.updateProfile(userId, request);
  }

  @PatchMapping
  public UserProfileResponse updateProfile(
      @RequestHeader("X-User-Id") UUID userId, @RequestBody UpdateUserProfileRequest request) {
    return userProfileService.updateProfile(userId, request);
  }

  @DeleteMapping("/{userId}")
  public void deleteUserProfile(@PathVariable UUID userId) {
    userProfileService.deleteProfile(userId);
  }
}
