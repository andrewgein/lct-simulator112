package com.simulator112.classifier.domain.model;

import java.util.List;
import java.util.Map;

public record RoutingResult(
        String classifierCode,
        String incidentTypeName,
        Map<String, String> facts,
        List<RoutingDecision> decisions) {
}
