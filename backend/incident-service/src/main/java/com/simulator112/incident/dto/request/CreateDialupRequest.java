package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.ApplicantRequest;
import com.simulator112.incident.dto.request.embeddable.DialupDetailsRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateDialupRequest(
    @NotNull
    @PositiveOrZero
    Integer position,

    @NotNull
    @Valid
    ApplicantRequest applicant,

    @NotNull
    @Valid
    DialupDetailsRequest dialupDetails
) {
}
