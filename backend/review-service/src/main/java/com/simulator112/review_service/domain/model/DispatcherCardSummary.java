package com.simulator112.review_service.domain.model;

import java.util.List;
import java.util.Map;

public record DispatcherCardSummary(String cardId, String callId, String mainCardId, String incidentId, ReviewSubmission.Person applicant,
                                    Integer victimCount, List<String> incidentTypes, List<String> services,
                                    Map<String, String> additionalInfo) {
    public DispatcherCardSummary {
        incidentTypes = List.copyOf(incidentTypes);
        services = List.copyOf(services);
        additionalInfo = Map.copyOf(additionalInfo);
    }
}
