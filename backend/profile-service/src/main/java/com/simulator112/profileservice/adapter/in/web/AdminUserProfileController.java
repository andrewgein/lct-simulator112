package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.AssignProfessionalProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UserProfileResponse;
import com.simulator112.profileservice.application.port.in.AssignProfessionalProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/profiles")
@RequiredArgsConstructor
public class AdminUserProfileController {

    private final AssignProfessionalProfileUseCase professionalProfiles;

    @PutMapping("/{userId}/professional-profile")
    public UserProfileResponse assignProfessionalProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignProfessionalProfileRequest request) {
        return UserProfileWebMapper.toResponse(
                professionalProfiles.assignProfessionalProfile(userId, request.toDomain()));
    }
}
