package com.simulator112.profileservice.adapter.in.web.dto;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.TrainingTrack;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        UUID authId,
        String name,
        String surname,
        TrainingTrack trainingTrack,
        DdsService ddsService,
        Instant updatedAt) {
}
