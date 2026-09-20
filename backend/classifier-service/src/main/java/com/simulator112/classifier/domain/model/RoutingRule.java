package com.simulator112.classifier.domain.model;

public record RoutingRule(
        RoutingVariant variant,
        RoutingResultKind resultKind,
        String targetTypeName) {
}
