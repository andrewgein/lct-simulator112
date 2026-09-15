package com.simulator112.incident.dto.view;

import com.simulator112.incident.dto.view.embeddable.ApplicantView;

import java.util.List;
import java.util.UUID;

public record StageView(
    UUID id,
    String title,
    Integer position,
    TypeView type,
    String description,
    ApplicantView victim,
    List<IncidentAdditionalInfoView> additionalInfo,
    List<DialupView> dialups
) {
}
