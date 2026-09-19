package com.simulator112.incident.dto.view.classifier;

import com.simulator112.incident.model.enums.classifier.RoutingResultKind;

public record RoutingDecisionView(
        DispatchServiceView service,
        String routingTarget,
        String matchedVariant,
        RoutingResultKind resultKind,
        String targetTypeName
) {
}
