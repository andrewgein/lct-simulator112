package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.VictimRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record CreateStageRequest(
    String title,

    @NotNull
    @PositiveOrZero
    Integer position,

    @NotNull
    UUID typeId,

    String description,

    @Valid
    VictimRequest victim,

    @NotNull
    @Valid
    List<IncidentAdditionalInfoRequest> additionalInfo
) {
}
