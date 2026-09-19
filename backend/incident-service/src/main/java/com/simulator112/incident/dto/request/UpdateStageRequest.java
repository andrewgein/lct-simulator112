package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.VictimRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStageRequest(
    String title,

    @PositiveOrZero
    Integer position,

    String classifierCode,

    String description,

    @Valid
    VictimRequest victim
) {
}
