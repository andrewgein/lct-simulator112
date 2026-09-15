package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.ApplicantRequest;
import com.simulator112.incident.dto.request.embeddable.DialupDetailsRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateDialupRequest(
    @PositiveOrZero
    Integer position,

    @Valid
    ApplicantRequest applicant,

    @Valid
    DialupDetailsRequest dialupDetails
) {
}
