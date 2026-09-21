package com.simulator112.classifier.domain.model;

public record RoutingDecision(
        DispatchService service,
        String routingTarget,
        String matchedVariant,
        RoutingResultKind resultKind,
        String targetTypeName) {
}
