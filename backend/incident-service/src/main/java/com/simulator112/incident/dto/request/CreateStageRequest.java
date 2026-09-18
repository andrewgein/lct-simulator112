package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.VictimRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateStageRequest(
    String title,

    @NotNull
    @PositiveOrZero
    Integer position,

    @NotBlank
    String classifierCode,

    String description,

    @Valid
    VictimRequest victim
) {
}
