package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.AssignProfessionalProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UserProfileResponse;
import com.simulator112.profileservice.application.port.in.AssignProfessionalProfileUseCase;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/profiles")
@RequiredArgsConstructor
public class AdminUserProfileController {

    private final AssignProfessionalProfileUseCase professionalProfiles;
    private final UserProfileUseCase userProfiles;

    @GetMapping
    public List<UserProfileResponse> getAllProfiles() {
        return userProfiles.getAllProfiles().stream()
                .map(UserProfileWebMapper::toResponse)
                .toList();
    }

    @PutMapping("/{userId}/professional-profile")
    public UserProfileResponse assignProfessionalProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignProfessionalProfileRequest request) {
        return UserProfileWebMapper.toResponse(
                professionalProfiles.assignProfessionalProfile(userId, request.toDomain()));
    }
}
