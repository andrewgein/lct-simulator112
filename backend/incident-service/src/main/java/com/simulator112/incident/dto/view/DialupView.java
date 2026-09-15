package com.simulator112.incident.dto.view;

import com.simulator112.incident.dto.view.embeddable.ApplicantView;
import com.simulator112.incident.dto.view.embeddable.DialupDetailsView;

import java.util.UUID;

public record DialupView(
    UUID id,

    Integer position,

    ApplicantView applicant,

    DialupDetailsView dialupDetails
) {
}
