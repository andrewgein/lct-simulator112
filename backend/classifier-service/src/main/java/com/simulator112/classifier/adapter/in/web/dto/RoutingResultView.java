package com.simulator112.classifier.adapter.in.web.dto;

import java.util.List;
import java.util.Map;

public record RoutingResultView(
        String classifierCode,
        String incidentTypeName,
        Map<String, String> facts,
        List<RoutingDecisionView> decisions
) {
}
