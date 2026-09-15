package com.simulator112.incident.dto.view;

import com.simulator112.incident.dto.view.embeddable.AddressView;
import com.simulator112.incident.dto.view.embeddable.DispatcherCriteriaView;

import java.util.List;
import java.util.UUID;

public record IncidentFullView(
    UUID id,
    String title,
    AddressView address,
    DispatcherCriteriaView criteria,
    UUID levelId,
    List<StageView> stages
) {
}
