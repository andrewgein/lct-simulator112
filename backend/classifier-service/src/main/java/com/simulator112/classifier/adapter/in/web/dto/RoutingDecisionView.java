package com.simulator112.classifier.adapter.in.web.dto;

import com.simulator112.classifier.domain.model.RoutingResultKind;

public record RoutingDecisionView(
        DispatchServiceView service,
        String routingTarget,
        String matchedVariant,
        RoutingResultKind resultKind,
        String targetTypeName
) {
}
