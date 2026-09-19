package com.simulator112.incident.dto.view;

import com.simulator112.incident.model.enums.RoutingResultKind;

public record RoutingDecisionView(
        DispatchServiceView service,
        String routingTarget,
        String matchedVariant,
        RoutingResultKind resultKind,
        String targetTypeName
) {
}
