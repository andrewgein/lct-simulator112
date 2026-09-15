package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.VictimRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record UpdateStageRequest(
    String title,

    @PositiveOrZero
    Integer position,

    UUID typeId,

    String description,

    @Valid
    VictimRequest victim,

    @Valid
    List<IncidentAdditionalInfoRequest> additionalInfo
) {
}
