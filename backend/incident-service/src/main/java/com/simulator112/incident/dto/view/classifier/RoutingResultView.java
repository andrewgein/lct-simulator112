package com.simulator112.incident.dto.view.classifier;

import java.util.List;
import java.util.Map;

public record RoutingResultView(
        String classifierCode,
        String incidentTypeName,
        Map<String, String> facts,
        List<RoutingDecisionView> decisions
) {
}
