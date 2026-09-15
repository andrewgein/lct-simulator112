package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.AddressRequest;
import com.simulator112.incident.dto.request.embeddable.DispatcherCriteriaRequest;

public record UpdateIncidentRequest(
    String title,
    AddressRequest address,
    DispatcherCriteriaRequest criteria
) {
}
