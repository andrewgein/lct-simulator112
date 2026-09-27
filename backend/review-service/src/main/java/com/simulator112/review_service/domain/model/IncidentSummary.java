package com.simulator112.review_service.domain.model;

import java.util.List;

public record IncidentSummary(String id, int order, String title, int victimCount, List<String> classifierCodes) {
    public IncidentSummary {
        classifierCodes = List.copyOf(classifierCodes);
    }
}
