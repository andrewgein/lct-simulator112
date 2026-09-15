package com.simulator112.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IncidentAdditionalInfoRequest(
    @NotNull
    UUID additionalInfoId,

    @NotBlank
    String fieldValue
) {
}
